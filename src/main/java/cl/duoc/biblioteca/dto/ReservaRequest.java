package cl.duoc.biblioteca.dto;

import jakarta.validation.constraints.NotNull;

public record ReservaRequest(
        @NotNull(message = "El id del libro es obligatorio")
        Long libroId
) {
}