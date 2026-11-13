package cl.duoc.biblioteca.dto;

import java.time.LocalDate;

public record UsuarioResponse(
        Long id,
        String nombreCompleto,
        String correo,
        LocalDate fechaNacimiento,
        String rol,
        String categoria
) {
}