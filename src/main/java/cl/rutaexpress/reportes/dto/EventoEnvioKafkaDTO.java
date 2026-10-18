package cl.rutaexpress.reportes.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventoEnvioKafkaDTO(
        UUID eventoId,
        Long envioId,
        String codigoSeguimiento,
        String estadoAnterior,
        String estadoNuevo,
        String usuarioEmail,
        String usuarioRol,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime timestamp,
        Map<String, Object> detalles
) {
}
