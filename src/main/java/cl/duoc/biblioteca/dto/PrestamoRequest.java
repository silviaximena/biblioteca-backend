package cl.duoc.biblioteca.dto;

import jakarta.validation.constraints.NotNull;

public record PrestamoRequest(
        @NotNull(message = "El id del libro es obligatorio")
        Long libroId
) {
}