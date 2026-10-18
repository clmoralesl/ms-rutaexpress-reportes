package cl.rutaexpress.reportes.controller;

import cl.rutaexpress.reportes.dto.KpisResumenResponse;
import cl.rutaexpress.reportes.dto.RendimientoCourierResponse;
import cl.rutaexpress.reportes.dto.TopServicioResponse;
import cl.rutaexpress.reportes.dto.VolumenHoraItem;
import cl.rutaexpress.reportes.service.ReportesService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ReportesController.class, SaludController.class})
class ReportesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReportesService reportesService;

    @Test
    @DisplayName("GET /api/reportes/kpis retorna resumen de indicadores")
    void testObtenerKpis() throws Exception {
        KpisResumenResponse mockKpis = new KpisResumenResponse(
                3.5,
                96.0,
                100L,
                30L,
                67L,
                3L,
                Map.of("CREADO", 10L, "ENTREGADO", 67L),
                List.of(new VolumenHoraItem("10:00", 5L))
        );

        when(reportesService.obtenerKpis(anyString())).thenReturn(mockKpis);

        mockMvc.perform(get("/api/reportes/kpis?rango=ultimas24h"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.leadTimePromedioHoras").value(3.5))
                .andExpect(jsonPath("$.tasaCumplimiento").value(96.0))
                .andExpect(jsonPath("$.totalEnvios").value(100))
                .andExpect(jsonPath("$.totalActivos").value(30))
                .andExpect(jsonPath("$.totalEntregados").value(67));
    }

    @Test
    @DisplayName("GET /api/reportes/top-servicios retorna lista ordenada")
    void testObtenerTopServicios() throws Exception {
        TopServicioResponse s1 = new TopServicioResponse("Envio Express 24h", 80L, 80.0);
        when(reportesService.obtenerTopServicios(anyString())).thenReturn(List.of(s1));

        mockMvc.perform(get("/api/reportes/top-servicios"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].servicioNombre").value("Envio Express 24h"))
                .andExpect(jsonPath("$[0].cantidadEnvios").value(80))
                .andExpect(jsonPath("$[0].porcentajeTotal").value(80.0));
    }

    @Test
    @DisplayName("GET /api/reportes/rendimiento-couriers retorna lista de operadores")
    void testObtenerRendimientoCouriers() throws Exception {
        RendimientoCourierResponse c1 = new RendimientoCourierResponse(
                "courier@rutaexpress.cl", "COURIER", 30L, 29L, 1L, 96.7
        );
        when(reportesService.obtenerRendimientoCouriers()).thenReturn(List.of(c1));

        mockMvc.perform(get("/api/reportes/rendimiento-couriers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].usuarioEmail").value("courier@rutaexpress.cl"))
                .andExpect(jsonPath("$[0].tasaExito").value(96.7));
    }

    @Test
    @DisplayName("GET /api/publico/salud retorna DISPONIBLE")
    void testEndpointSalud() throws Exception {
        mockMvc.perform(get("/api/publico/salud"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("DISPONIBLE"))
                .andExpect(jsonPath("$.servicio").value("ms-rutaexpress-reportes"))
                .andExpect(jsonPath("$.puerto").value(8086));
    }
}
