package cl.rutaexpress.reportes.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/publico/salud")
public class SaludController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> verificarSalud() {
        Map<String, Object> salud = new LinkedHashMap<>();
        salud.put("estado", "DISPONIBLE");
        salud.put("servicio", "ms-rutaexpress-reportes");
        salud.put("puerto", 8086);
        salud.put("timestamp", LocalDateTime.now());
        salud.put("descripcion", "Microservicio consumidor Kafka y motor analitico de KPIs operacionales");
        return ResponseEntity.ok(salud);
    }
}
