package cl.duoc.biblioteca.dto;

import java.time.LocalDate;

public record PrestamoResponse(
        Long id,
        Long usuarioId,
        String usuarioNombre,
        String usuarioCorreo,
        Long libroId,
        String libroTitulo,
        String codigoEjemplar,
        LocalDate fechaPrestamo,
        LocalDate fechaVencimiento,
        LocalDate fechaDevolucion,
        String estado,
        boolean atrasado
) {
}