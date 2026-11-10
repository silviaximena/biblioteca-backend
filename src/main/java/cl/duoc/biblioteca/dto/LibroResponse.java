package cl.duoc.biblioteca.dto;

public record LibroResponse(
        Long id,
        String titulo,
        String autor,
        String isbn,
        Integer anio,
        String portada,
        String categoria,
        String editorial,
        long stock,
        long disponibles
) {
}