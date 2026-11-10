package cl.duoc.biblioteca.repository;

import cl.duoc.biblioteca.model.Ejemplar;
import cl.duoc.biblioteca.model.EstadoEjemplar;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EjemplarRepository extends JpaRepository<Ejemplar, Long> {

    long countByLibroId(Long libroId);

    long countByLibroIdAndEstado(Long libroId, EstadoEjemplar estado);

    Optional<Ejemplar> findFirstByLibroIdAndEstado(Long libroId, EstadoEjemplar estado);

    List<Ejemplar> findByLibroId(Long libroId);

    List<Ejemplar> findByLibroIdAndEstado(Long libroId, EstadoEjemplar estado);
}