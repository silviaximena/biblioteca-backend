package cl.duoc.biblioteca.repository;

import cl.duoc.biblioteca.model.Editorial;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EditorialRepository extends JpaRepository<Editorial, Long> {

    Optional<Editorial> findByNombreIgnoreCase(String nombre);
}