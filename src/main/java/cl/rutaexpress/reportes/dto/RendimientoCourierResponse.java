package cl.rutaexpress.reportes.dto;

public record RendimientoCourierResponse(
        String usuarioEmail,
        String usuarioRol,
        long totalGestionados,
        long totalEntregados,
        long totalCancelados,
        Double tasaExito
) {
}
