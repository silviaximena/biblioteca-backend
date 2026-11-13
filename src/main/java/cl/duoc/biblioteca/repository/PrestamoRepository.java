package cl.duoc.biblioteca.repository;

import cl.duoc.biblioteca.model.EstadoPrestamo;
import cl.duoc.biblioteca.model.Prestamo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {

    List<Prestamo> findByUsuarioCorreo(String correo);

    List<Prestamo> findByUsuarioCorreoOrderByIdDesc(String correo);

    long countByUsuarioIdAndEstadoIn(Long usuarioId, Collection<EstadoPrestamo> estados);

    boolean existsByUsuarioIdAndEjemplarLibroIdAndEstadoIn(Long usuarioId, Long libroId, Collection<EstadoPrestamo> estados);

    boolean existsByEjemplarId(Long ejemplarId);

    boolean existsByEjemplarLibroId(Long libroId);
}