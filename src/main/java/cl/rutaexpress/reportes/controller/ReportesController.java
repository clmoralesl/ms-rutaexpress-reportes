package cl.rutaexpress.reportes.controller;

import cl.rutaexpress.reportes.dto.KpisResumenResponse;
import cl.rutaexpress.reportes.dto.RendimientoCourierResponse;
import cl.rutaexpress.reportes.dto.TopServicioResponse;
import cl.rutaexpress.reportes.service.ReportesService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/reportes")
public class ReportesController {

    private final ReportesService reportesService;

    public ReportesController(ReportesService reportesService) {
        this.reportesService = reportesService;
    }

    @GetMapping("/kpis")
    public ResponseEntity<KpisResumenResponse> obtenerKpis(
            @RequestParam(required = false, defaultValue = "ultimas24h") String rango) {
        return ResponseEntity.ok(reportesService.obtenerKpis(rango));
    }

    @GetMapping("/top-servicios")
    public ResponseEntity<List<TopServicioResponse>> obtenerTopServicios(
            @RequestParam(required = false, defaultValue = "ultimos7dias") String rango) {
        return ResponseEntity.ok(reportesService.obtenerTopServicios(rango));
    }

    @GetMapping("/rendimiento-couriers")
    public ResponseEntity<List<RendimientoCourierResponse>> obtenerRendimientoCouriers() {
        return ResponseEntity.ok(reportesService.obtenerRendimientoCouriers());
    }
}
