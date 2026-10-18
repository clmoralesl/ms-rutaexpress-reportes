package cl.rutaexpress.reportes.repository;

import cl.rutaexpress.reportes.entity.RegistroAnaliticoEnvio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RegistroAnaliticoEnvioRepository extends JpaRepository<RegistroAnaliticoEnvio, Long> {

    Optional<RegistroAnaliticoEnvio> findByEnvioId(Long envioId);

    @Query("SELECT AVG(r.leadTimeHoras) FROM RegistroAnaliticoEnvio r WHERE r.leadTimeHoras IS NOT NULL AND r.fechaCreacion >= :desde")
    Double calcularLeadTimePromedioDesde(@Param("desde") LocalDateTime desde);

    @Query("SELECT AVG(r.leadTimeHoras) FROM RegistroAnaliticoEnvio r WHERE r.leadTimeHoras IS NOT NULL")
    Double calcularLeadTimePromedioGeneral();

    @Query("SELECT r.estadoActual, COUNT(r) FROM RegistroAnaliticoEnvio r WHERE r.fechaCreacion >= :desde GROUP BY r.estadoActual")
    List<Object[]> contarPorEstadoDesde(@Param("desde") LocalDateTime desde);

    @Query("SELECT r.estadoActual, COUNT(r) FROM RegistroAnaliticoEnvio r GROUP BY r.estadoActual")
    List<Object[]> contarPorEstadoGeneral();

    @Query("SELECT r.servicioNombre, COUNT(r) FROM RegistroAnaliticoEnvio r WHERE r.fechaCreacion >= :desde GROUP BY r.servicioNombre ORDER BY COUNT(r) DESC")
    List<Object[]> rankingServiciosDesde(@Param("desde") LocalDateTime desde);

    @Query("SELECT r.servicioNombre, COUNT(r) FROM RegistroAnaliticoEnvio r GROUP BY r.servicioNombre ORDER BY COUNT(r) DESC")
    List<Object[]> rankingServiciosGeneral();

    @Query("SELECT r.ultimoOperadorEmail, r.ultimoOperadorRol, COUNT(r), " +
           "SUM(CASE WHEN r.estadoActual = 'ENTREGADO' THEN 1L ELSE 0L END), " +
           "SUM(CASE WHEN r.estadoActual = 'CANCELADO' THEN 1L ELSE 0L END) " +
           "FROM RegistroAnaliticoEnvio r " +
           "WHERE r.ultimoOperadorEmail IS NOT NULL AND r.ultimoOperadorEmail <> '' " +
           "GROUP BY r.ultimoOperadorEmail, r.ultimoOperadorRol")
    List<Object[]> metricasRendimientoOperadores();

    List<RegistroAnaliticoEnvio> findByFechaCreacionGreaterThanEqualOrderByFechaCreacionAsc(LocalDateTime desde);
}
