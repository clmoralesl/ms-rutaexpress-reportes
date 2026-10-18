package cl.rutaexpress.reportes.service;

import cl.rutaexpress.reportes.dto.*;
import cl.rutaexpress.reportes.entity.RegistroAnaliticoEnvio;
import cl.rutaexpress.reportes.repository.RegistroAnaliticoEnvioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ReportesService {

    private static final Logger log = LoggerFactory.getLogger(ReportesService.class);

    private final RegistroAnaliticoEnvioRepository repository;

    public ReportesService(RegistroAnaliticoEnvioRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public RegistroAnaliticoEnvio procesarEvento(EventoEnvioKafkaDTO dto) {
        if (dto == null || dto.envioId() == null) {
            log.warn("Se intento procesar un evento analitico invalido o sin envioId.");
            return null;
        }

        Optional<RegistroAnaliticoEnvio> existenteOpt = repository.findByEnvioId(dto.envioId());
        RegistroAnaliticoEnvio registro;

        if (existenteOpt.isPresent()) {
            registro = existenteOpt.get();
            registro.actualizarTransicion(
                    dto.estadoNuevo(),
                    dto.timestamp(),
                    dto.usuarioEmail(),
                    dto.usuarioRol()
            );
        } else {
            String servicio = extraerNombreServicio(dto.detalles());
            registro = new RegistroAnaliticoEnvio(
                    dto.envioId(),
                    dto.codigoSeguimiento(),
                    dto.estadoNuevo(),
                    dto.timestamp(),
                    servicio,
                    dto.usuarioEmail(),
                    dto.usuarioRol()
            );
            if ("ENTREGADO".equalsIgnoreCase(dto.estadoNuevo())) {
                registro.actualizarTransicion(dto.estadoNuevo(), dto.timestamp(), dto.usuarioEmail(), dto.usuarioRol());
            }
        }

        RegistroAnaliticoEnvio guardado = repository.save(registro);
        log.info("Registro analitico actualizado [envioId={}, estadoActual={}, leadTimeHoras={}]",
                guardado.getEnvioId(), guardado.getEstadoActual(), guardado.getLeadTimeHoras());

        return guardado;
    }

    public KpisResumenResponse obtenerKpis(String rango) {
        LocalDateTime desde = calcularFechaDesde(rango);

        Double leadTimeRaw = repository.calcularLeadTimePromedioDesde(desde);
        Double leadTimePromedio = (leadTimeRaw != null) ? redondear(leadTimeRaw, 2) : 0.0;

        List<Object[]> estadosRaw = repository.contarPorEstadoDesde(desde);
        Map<String, Long> distribucion = new LinkedHashMap<>();
        distribucion.put("CREADO", 0L);
        distribucion.put("ACEPTADO", 0L);
        distribucion.put("EN_BODEGA", 0L);
        distribucion.put("EN_RUTA", 0L);
        distribucion.put("ENTREGADO", 0L);
        distribucion.put("CANCELADO", 0L);

        long totalEnvios = 0;
        for (Object[] fila : estadosRaw) {
            String estado = (String) fila[0];
            Long cantidad = (Long) fila[1];
            if (estado != null) {
                distribucion.put(estado.toUpperCase(), cantidad);
                totalEnvios += cantidad;
            }
        }

        long entregados = distribucion.getOrDefault("ENTREGADO", 0L);
        long cancelados = distribucion.getOrDefault("CANCELADO", 0L);
        long activos = distribucion.getOrDefault("CREADO", 0L)
                + distribucion.getOrDefault("ACEPTADO", 0L)
                + distribucion.getOrDefault("EN_BODEGA", 0L)
                + distribucion.getOrDefault("EN_RUTA", 0L);

        Double tasaCumplimiento = 100.0;
        if ((entregados + cancelados) > 0) {
            double tasa = (entregados * 100.0) / (entregados + cancelados);
            tasaCumplimiento = redondear(tasa, 1);
        }

        List<VolumenHoraItem> volumenHorario = calcularVolumenHorario(desde);

        return new KpisResumenResponse(
                leadTimePromedio,
                tasaCumplimiento,
                totalEnvios,
                activos,
                entregados,
                cancelados,
                distribucion,
                volumenHorario
        );
    }

    public List<TopServicioResponse> obtenerTopServicios(String rango) {
        LocalDateTime desde = calcularFechaDesde(rango);
        List<Object[]> filas = repository.rankingServiciosDesde(desde);

        long total = 0;
        for (Object[] fila : filas) {
            total += (Long) fila[1];
        }

        List<TopServicioResponse> resultado = new ArrayList<>();
        for (Object[] fila : filas) {
            String nombre = (String) fila[0];
            Long cantidad = (Long) fila[1];
            double porcentaje = (total > 0) ? (cantidad * 100.0) / total : 0.0;
            resultado.add(new TopServicioResponse(nombre, cantidad, redondear(porcentaje, 1)));
        }

        return resultado;
    }

    public List<RendimientoCourierResponse> obtenerRendimientoCouriers() {
        List<Object[]> filas = repository.metricasRendimientoOperadores();
        List<RendimientoCourierResponse> resultado = new ArrayList<>();

        for (Object[] fila : filas) {
            String email = (String) fila[0];
            String rol = (String) fila[1];
            Long total = (Long) fila[2];
            Long entregados = (Long) fila[3];
            Long cancelados = (Long) fila[4];

            double tasa = (total > 0) ? (entregados * 100.0) / total : 0.0;

            resultado.add(new RendimientoCourierResponse(
                    email,
                    rol != null ? rol : "OPERADOR",
                    total,
                    entregados,
                    cancelados,
                    redondear(tasa, 1)
            ));
        }

        return resultado;
    }

    private List<VolumenHoraItem> calcularVolumenHorario(LocalDateTime desde) {
        List<RegistroAnaliticoEnvio> envios = repository.findByFechaCreacionGreaterThanEqualOrderByFechaCreacionAsc(desde);
        Map<String, Long> acumuladorHoras = new LinkedHashMap<>();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:00");
        for (RegistroAnaliticoEnvio envio : envios) {
            if (envio.getFechaCreacion() != null) {
                String horaStr = envio.getFechaCreacion().format(formatter);
                acumuladorHoras.put(horaStr, acumuladorHoras.getOrDefault(horaStr, 0L) + 1);
            }
        }

        List<VolumenHoraItem> items = new ArrayList<>();
        acumuladorHoras.forEach((hora, cant) -> items.add(new VolumenHoraItem(hora, cant)));
        return items;
    }

    private LocalDateTime calcularFechaDesde(String rango) {
        if (rango == null) {
            return LocalDateTime.now().minusHours(24);
        }
        return switch (rango.toLowerCase().trim()) {
            case "ultimos7dias", "7dias" -> LocalDateTime.now().minusDays(7);
            case "ultimomes", "30dias" -> LocalDateTime.now().minusDays(30);
            case "historico", "todo" -> LocalDateTime.of(2020, 1, 1, 0, 0);
            default -> LocalDateTime.now().minusHours(24);
        };
    }

    private String extraerNombreServicio(Map<String, Object> detalles) {
        if (detalles == null) {
            return "Envio Estandar 24h";
        }
        if (detalles.containsKey("servicio")) {
            return String.valueOf(detalles.get("servicio"));
        }
        if (detalles.containsKey("nombreServicio")) {
            return String.valueOf(detalles.get("nombreServicio"));
        }
        if (detalles.containsKey("tipoServicio")) {
            return String.valueOf(detalles.get("tipoServicio"));
        }
        return "Envio Express 24h";
    }

    private Double redondear(Double valor, int decimales) {
        if (valor == null) {
            return 0.0;
        }
        return BigDecimal.valueOf(valor)
                .setScale(decimales, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
