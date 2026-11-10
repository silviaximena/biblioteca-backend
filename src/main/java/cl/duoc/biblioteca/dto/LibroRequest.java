package cl.duoc.biblioteca.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record LibroRequest(
        @NotBlank(message = "El título es obligatorio")
        @Size(max = 150, message = "El título no puede superar 150 caracteres")
        String titulo,

        @NotBlank(message = "El autor es obligatorio")
        String autor,

        @Size(max = 20, message = "El ISBN no puede superar 20 caracteres")
        String isbn,

        @Min(value = 1000, message = "El año debe tener 4 dígitos")
        @Max(value = 2100, message = "El año no es válido")
        Integer anio,

        @Size(max = 500, message = "La URL de la portada es demasiado larga")
        String portada,

        @NotBlank(message = "La categoría es obligatoria")
        String categoria,

        String editorial,

        @NotNull(message = "El stock es obligatorio")
        @Min(value = 0, message = "El stock no puede ser negativo")
        Integer stock
) {
}