package cl.duoc.biblioteca.repository;

import cl.duoc.biblioteca.model.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByUsuarioCorreo(String correo);

    boolean existsByLibroId(Long libroId);
}