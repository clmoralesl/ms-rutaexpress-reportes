package cl.rutaexpress.reportes.dto;

import java.util.List;
import java.util.Map;

public record KpisResumenResponse(
        Double leadTimePromedioHoras,
        Double tasaCumplimiento,
        long totalEnvios,
        long totalActivos,
        long totalEntregados,
        long totalCancelados,
        Map<String, Long> distribucionPorEstado,
        List<VolumenHoraItem> volumenPorHora
) {
}
