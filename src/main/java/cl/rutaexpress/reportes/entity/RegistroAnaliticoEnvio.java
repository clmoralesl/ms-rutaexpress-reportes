package cl.rutaexpress.reportes.entity;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "REGISTRO_ANALITICO_ENVIOS",
        indexes = {
                @Index(name = "idx_analitico_envio_id", columnList = "envio_id"),
                @Index(name = "idx_analitico_estado", columnList = "estado_actual"),
                @Index(name = "idx_analitico_fecha_creacion", columnList = "fecha_creacion"),
                @Index(name = "idx_analitico_operador", columnList = "ultimo_operador_email")
        }
)
public class RegistroAnaliticoEnvio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "envio_id", nullable = false, unique = true)
    private Long envioId;

    @Column(name = "codigo_seguimiento", nullable = false, length = 50)
    private String codigoSeguimiento;

    @Column(name = "estado_actual", nullable = false, length = 30)
    private String estadoActual;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_entrega")
    private LocalDateTime fechaEntrega;

    @Column(name = "lead_time_horas")
    private Double leadTimeHoras;

    @Column(name = "servicio_nombre", length = 100)
    private String servicioNombre;

    @Column(name = "ultimo_operador_email", length = 100)
    private String ultimoOperadorEmail;

    @Column(name = "ultimo_operador_rol", length = 50)
    private String ultimoOperadorRol;

    @Column(name = "ultima_actualizacion", nullable = false)
    private LocalDateTime ultimaActualizacion;

    protected RegistroAnaliticoEnvio() {
    }

    public RegistroAnaliticoEnvio(
            Long envioId,
            String codigoSeguimiento,
            String estadoActual,
            LocalDateTime fechaCreacion,
            String servicioNombre,
            String ultimoOperadorEmail,
            String ultimoOperadorRol) {
        this.envioId = envioId;
        this.codigoSeguimiento = codigoSeguimiento;
        this.estadoActual = estadoActual;
        this.fechaCreacion = (fechaCreacion != null) ? fechaCreacion : LocalDateTime.now();
        this.servicioNombre = (servicioNombre != null && !servicioNombre.isBlank()) ? servicioNombre : "Envio Estandar 24h";
        this.ultimoOperadorEmail = ultimoOperadorEmail;
        this.ultimoOperadorRol = ultimoOperadorRol;
        this.ultimaActualizacion = LocalDateTime.now();
    }

    public void actualizarTransicion(
            String nuevoEstado,
            LocalDateTime fechaEvento,
            String operadorEmail,
            String operadorRol) {
        this.estadoActual = nuevoEstado;
        this.ultimaActualizacion = LocalDateTime.now();
        if (operadorEmail != null && !operadorEmail.isBlank()) {
            this.ultimoOperadorEmail = operadorEmail;
        }
        if (operadorRol != null && !operadorRol.isBlank()) {
            this.ultimoOperadorRol = operadorRol;
        }

        if ("ENTREGADO".equalsIgnoreCase(nuevoEstado)) {
            this.fechaEntrega = (fechaEvento != null) ? fechaEvento : LocalDateTime.now();
            if (this.fechaCreacion != null) {
                long minutos = Duration.between(this.fechaCreacion, this.fechaEntrega).toMinutes();
                this.leadTimeHoras = Math.max(0.01, minutos / 60.0);
            }
        }
    }

    public Long getId() {
        return id;
    }

    public Long getEnvioId() {
        return envioId;
    }

    public String getCodigoSeguimiento() {
        return codigoSeguimiento;
    }

    public String getEstadoActual() {
        return estadoActual;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaEntrega() {
        return fechaEntrega;
    }

    public Double getLeadTimeHoras() {
        return leadTimeHoras;
    }

    public String getServicioNombre() {
        return servicioNombre;
    }

    public String getUltimoOperadorEmail() {
        return ultimoOperadorEmail;
    }

    public String getUltimoOperadorRol() {
        return ultimoOperadorRol;
    }

    public LocalDateTime getUltimaActualizacion() {
        return ultimaActualizacion;
    }
}
