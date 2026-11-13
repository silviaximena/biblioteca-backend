package cl.duoc.biblioteca.dto;

import java.time.LocalDate;

public record ReservaResponse(
        Long id,
        Long usuarioId,
        String usuarioNombre,
        String usuarioCorreo,
        Long libroId,
        String libroTitulo,
        LocalDate fechaReserva,
        LocalDate fechaVigencia,
        String estado
) {
}