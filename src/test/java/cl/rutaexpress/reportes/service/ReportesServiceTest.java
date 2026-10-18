package cl.rutaexpress.reportes.service;

import cl.rutaexpress.reportes.dto.*;
import cl.rutaexpress.reportes.entity.RegistroAnaliticoEnvio;
import cl.rutaexpress.reportes.repository.RegistroAnaliticoEnvioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportesServiceTest {

    @Mock
    private RegistroAnaliticoEnvioRepository repository;

    @Captor
    private ArgumentCaptor<RegistroAnaliticoEnvio> captor;

    private ReportesService service;

    @BeforeEach
    void setUp() {
        service = new ReportesService(repository);
    }

    @Test
    @DisplayName("Procesar evento CREADO: crea nuevo registro analitico")
    void testProcesarEventoCreado() {
        UUID eventoId = UUID.randomUUID();
        Map<String, Object> detalles = Map.of("servicio", "Envio Express 24h");
        LocalDateTime fecha = LocalDateTime.now().minusHours(2);

        EventoEnvioKafkaDTO dto = new EventoEnvioKafkaDTO(
                eventoId,
                101L,
                "RE-ANL-101",
                null,
                "CREADO",
                "operador@rutaexpress.cl",
                "OPERADOR",
                fecha,
                detalles
        );

        when(repository.findByEnvioId(101L)).thenReturn(Optional.empty());
        when(repository.save(any(RegistroAnaliticoEnvio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistroAnaliticoEnvio resultado = service.procesarEvento(dto);

        assertNotNull(resultado);
        verify(repository, times(1)).save(captor.capture());
        RegistroAnaliticoEnvio guardado = captor.getValue();
        assertEquals(101L, guardado.getEnvioId());
        assertEquals("CREADO", guardado.getEstadoActual());
        assertEquals("Envio Express 24h", guardado.getServicioNombre());
        assertNull(guardado.getLeadTimeHoras());
    }

    @Test
    @DisplayName("Procesar evento ENTREGADO: calcula Lead Time en horas")
    void testProcesarEventoEntregadoCalculaLeadTime() {
        LocalDateTime creacion = LocalDateTime.now().minusHours(5);
        RegistroAnaliticoEnvio existente = new RegistroAnaliticoEnvio(
                102L,
                "RE-ANL-102",
                "EN_RUTA",
                creacion,
                "Envio Mismo Dia",
                "courier@rutaexpress.cl",
                "COURIER"
        );

        LocalDateTime entrega = LocalDateTime.now();
        EventoEnvioKafkaDTO dto = new EventoEnvioKafkaDTO(
                UUID.randomUUID(),
                102L,
                "RE-ANL-102",
                "EN_RUTA",
                "ENTREGADO",
                "courier@rutaexpress.cl",
                "COURIER",
                entrega,
                Collections.emptyMap()
        );

        when(repository.findByEnvioId(102L)).thenReturn(Optional.of(existente));
        when(repository.save(any(RegistroAnaliticoEnvio.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RegistroAnaliticoEnvio resultado = service.procesarEvento(dto);

        assertNotNull(resultado);
        assertEquals("ENTREGADO", resultado.getEstadoActual());
        assertNotNull(resultado.getLeadTimeHoras());
        assertTrue(resultado.getLeadTimeHoras() >= 4.9 && resultado.getLeadTimeHoras() <= 5.1,
                "El Lead Time debe ser de aproximadamente 5.0 horas");
    }

    @Test
    @DisplayName("Obtener KPIs: calcula correctamente agregados, tasa y distribucion")
    void testObtenerKpis() {
        when(repository.calcularLeadTimePromedioDesde(any(LocalDateTime.class))).thenReturn(4.256);

        List<Object[]> conteoEstados = new ArrayList<>();
        conteoEstados.add(new Object[]{"CREADO", 10L});
        conteoEstados.add(new Object[]{"ACEPTADO", 5L});
        conteoEstados.add(new Object[]{"EN_BODEGA", 15L});
        conteoEstados.add(new Object[]{"EN_RUTA", 20L});
        conteoEstados.add(new Object[]{"ENTREGADO", 95L});
        conteoEstados.add(new Object[]{"CANCELADO", 5L});

        when(repository.contarPorEstadoDesde(any(LocalDateTime.class))).thenReturn(conteoEstados);

        RegistroAnaliticoEnvio envioMock = new RegistroAnaliticoEnvio(
                1L, "RE-1", "CREADO", LocalDateTime.now(), "Express", "op@mail.cl", "OP"
        );
        when(repository.findByFechaCreacionGreaterThanEqualOrderByFechaCreacionAsc(any(LocalDateTime.class)))
                .thenReturn(List.of(envioMock));

        KpisResumenResponse kpis = service.obtenerKpis("ultimas24h");

        assertNotNull(kpis);
        assertEquals(4.26, kpis.leadTimePromedioHoras());
        assertEquals(95.0, kpis.tasaCumplimiento()); // 95 / (95 + 5) * 100
        assertEquals(150L, kpis.totalEnvios());
        assertEquals(50L, kpis.totalActivos()); // 10 + 5 + 15 + 20
        assertEquals(95L, kpis.totalEntregados());
        assertEquals(5L, kpis.totalCancelados());
        assertFalse(kpis.volumenPorHora().isEmpty());
    }

    @Test
    @DisplayName("Obtener Top Servicios: retorna ranking ordenado con porcentajes")
    void testObtenerTopServicios() {
        List<Object[]> ranking = new ArrayList<>();
        ranking.add(new Object[]{"Envio Express 24h", 60L});
        ranking.add(new Object[]{"Envio Estandar 48h", 40L});

        when(repository.rankingServiciosDesde(any(LocalDateTime.class))).thenReturn(ranking);

        List<TopServicioResponse> resultado = service.obtenerTopServicios("ultimos7dias");

        assertNotNull(resultado);
        assertEquals(2, resultado.size());
        assertEquals("Envio Express 24h", resultado.get(0).servicioNombre());
        assertEquals(60L, resultado.get(0).cantidadEnvios());
        assertEquals(60.0, resultado.get(0).porcentajeTotal());
        assertEquals(40.0, resultado.get(1).porcentajeTotal());
    }

    @Test
    @DisplayName("Obtener Rendimiento Couriers: calcula tasa de exito")
    void testObtenerRendimientoCouriers() {
        List<Object[]> metricas = new ArrayList<>();
        metricas.add(new Object[]{"courier1@rutaexpress.cl", "COURIER", 50L, 45L, 5L});

        when(repository.metricasRendimientoOperadores()).thenReturn(metricas);

        List<RendimientoCourierResponse> resultado = service.obtenerRendimientoCouriers();

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("courier1@rutaexpress.cl", resultado.get(0).usuarioEmail());
        assertEquals(50L, resultado.get(0).totalGestionados());
        assertEquals(45L, resultado.get(0).totalEntregados());
        assertEquals(90.0, resultado.get(0).tasaExito()); // 45 / 50 * 100
    }
}
