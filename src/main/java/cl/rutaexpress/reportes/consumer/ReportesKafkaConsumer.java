package cl.rutaexpress.reportes.consumer;

import cl.rutaexpress.reportes.dto.EventoEnvioKafkaDTO;
import cl.rutaexpress.reportes.service.ReportesService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ReportesKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(ReportesKafkaConsumer.class);

    private final ReportesService reportesService;

    public ReportesKafkaConsumer(ReportesService reportesService) {
        this.reportesService = reportesService;
    }

    @KafkaListener(
            topics = "${kafka.topic.shipments-events:shipments.events}",
            groupId = "${spring.kafka.consumer.group-id:reportes-group}"
    )
    public void consumirEvento(ConsumerRecord<String, EventoEnvioKafkaDTO> record) {
        if (record == null || record.value() == null) {
            log.warn("Mensaje recibido nulo en reportes desde shipments.events. Se descarta.");
            return;
        }

        EventoEnvioKafkaDTO evento = record.value();
        String clave = record.key();

        log.info("Evento recibido en motor analitico [topico={}, particion={}, offset={}, clave={}, envioId={}, estadoNuevo={}]",
                record.topic(), record.partition(), record.offset(), clave, evento.envioId(), evento.estadoNuevo());

        try {
            reportesService.procesarEvento(evento);
        } catch (Exception ex) {
            log.error("Fallo al actualizar motor analitico de reportes para el envioId={}: {}",
                    evento.envioId(), ex.getMessage(), ex);
        }
    }
}
