package cl.duoc.biblioteca.repository;

import cl.duoc.biblioteca.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LibroRepository extends JpaRepository<Libro, Long> {

    List<Libro> findByCategoriaNombreIgnoreCase(String nombreCategoria);

    List<Libro> findByTituloContainingIgnoreCase(String texto);
}