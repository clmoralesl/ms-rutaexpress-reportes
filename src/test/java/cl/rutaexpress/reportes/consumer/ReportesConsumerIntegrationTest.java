package cl.rutaexpress.reportes.consumer;

import cl.rutaexpress.reportes.dto.EventoEnvioKafkaDTO;
import cl.rutaexpress.reportes.dto.KpisResumenResponse;
import cl.rutaexpress.reportes.entity.RegistroAnaliticoEnvio;
import cl.rutaexpress.reportes.repository.RegistroAnaliticoEnvioRepository;
import cl.rutaexpress.reportes.service.ReportesService;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReportesConsumerIntegrationTest {

    @Autowired
    private ReportesService reportesService;

    @Autowired
    private RegistroAnaliticoEnvioRepository repository;

    private KafkaTemplate<String, Object> testKafkaTemplate;

    @BeforeEach
    void setUpProducer() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        DefaultKafkaProducerFactory<String, Object> pf = new DefaultKafkaProducerFactory<>(props);
        testKafkaTemplate = new KafkaTemplate<>(pf);
    }

    @AfterEach
    void tearDown() {
        if (testKafkaTemplate != null) {
            testKafkaTemplate.destroy();
        }
    }

    @Test
    @DisplayName("Integracion con Cluster Kafka Real: Flujo CREADO -> ENTREGADO actualiza Lead Time y KPIs en tiempo real")
    void testFlujoEventosKafkaYRecalculoKpis() throws Exception {
        Long envioId = 2000L + new Random().nextInt(1000);
        String codigo = "RE-ANL-TEST-" + envioId;
        LocalDateTime creacion = LocalDateTime.now().minusHours(3);

        Map<String, Object> detalles = Map.of("servicio", "Envio Express 24h");

        // 1. Emitir evento CREADO hacia shipments.events
        EventoEnvioKafkaDTO eventoCreado = new EventoEnvioKafkaDTO(
                UUID.randomUUID(),
                envioId,
                codigo,
                null,
                "CREADO",
                "courier_test@rutaexpress.cl",
                "OPERADOR",
                creacion,
                detalles
        );
        testKafkaTemplate.send("shipments.events", String.valueOf(envioId), eventoCreado).get();

        // 2. Esperar que se registre la creacion
        long finCreacion = System.currentTimeMillis() + 8000;
        RegistroAnaliticoEnvio reg = null;
        while (System.currentTimeMillis() < finCreacion) {
            Optional<RegistroAnaliticoEnvio> opt = repository.findByEnvioId(envioId);
            if (opt.isPresent()) {
                reg = opt.get();
                break;
            }
            Thread.sleep(200);
        }
        assertNotNull(reg, "El evento de creacion debe haber sido consumido y persistido");
        assertEquals("CREADO", reg.getEstadoActual());

        // 3. Emitir evento ENTREGADO (3 horas despues de creacion)
        LocalDateTime entrega = LocalDateTime.now();
        EventoEnvioKafkaDTO eventoEntregado = new EventoEnvioKafkaDTO(
                UUID.randomUUID(),
                envioId,
                codigo,
                "EN_RUTA",
                "ENTREGADO",
                "courier_test@rutaexpress.cl",
                "COURIER",
                entrega,
                detalles
        );
        testKafkaTemplate.send("shipments.events", String.valueOf(envioId), eventoEntregado).get();

        // 4. Esperar que el consumidor de reportes procese la entrega y calcule Lead Time
        long finEntrega = System.currentTimeMillis() + 8000;
        RegistroAnaliticoEnvio regEntregado = null;
        while (System.currentTimeMillis() < finEntrega) {
            Optional<RegistroAnaliticoEnvio> opt = repository.findByEnvioId(envioId);
            if (opt.isPresent() && "ENTREGADO".equals(opt.get().getEstadoActual()) && opt.get().getLeadTimeHoras() != null) {
                regEntregado = opt.get();
                break;
            }
            Thread.sleep(200);
        }

        assertNotNull(regEntregado, "El evento de entrega debe haber sido consumido con calculo de Lead Time");
        assertEquals("ENTREGADO", regEntregado.getEstadoActual());
        assertNotNull(regEntregado.getLeadTimeHoras());
        assertTrue(regEntregado.getLeadTimeHoras() >= 2.8 && regEntregado.getLeadTimeHoras() <= 3.2,
                "El Lead Time calculado debe ser de aproximadamente 3.0 horas");

        // 5. Validar impacto en el servicio de KPIs
        KpisResumenResponse kpis = reportesService.obtenerKpis("historico");
        assertNotNull(kpis);
        assertTrue(kpis.totalEntregados() >= 1);
        assertTrue(kpis.leadTimePromedioHoras() > 0.0);
    }
}
