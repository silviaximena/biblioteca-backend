package cl.duoc.biblioteca.dto;

import java.time.LocalDate;

public record AuthResponse(
        String token,
        Long id,
        String nombreCompleto,
        String correo,
        LocalDate fechaNacimiento,
        String rol
) {
}