package cl.rutaexpress.reportes.dto;

public record TopServicioResponse(
        String servicioNombre,
        long cantidadEnvios,
        Double porcentajeTotal
) {
}
