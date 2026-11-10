package cl.duoc.biblioteca.controller;

import cl.duoc.biblioteca.dto.LibroRequest;
import cl.duoc.biblioteca.dto.LibroResponse;
import cl.duoc.biblioteca.service.LibroService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/libros")
@RequiredArgsConstructor
@Tag(name = "Libros", description = "Catálogo de libros de la biblioteca")
public class LibroController {

    private final LibroService libroService;

    @GetMapping
    @Operation(summary = "Listar libros, con filtro opcional por categoría o título")
    public List<LibroResponse> listar(
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String texto) {
        return libroService.listar(categoria, texto);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un libro por su id")
    public LibroResponse buscarPorId(@PathVariable Long id) {
        return libroService.buscarPorId(id);
    }

    @PostMapping
    @Operation(summary = "Agregar un libro y crear sus ejemplares")
    public ResponseEntity<LibroResponse> crear(@Valid @RequestBody LibroRequest datos) {
        return ResponseEntity.status(HttpStatus.CREATED).body(libroService.crear(datos));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar un libro y ajustar su stock")
    public LibroResponse actualizar(@PathVariable Long id, @Valid @RequestBody LibroRequest datos) {
        return libroService.actualizar(id, datos);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar un libro sin préstamos ni reservas")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        libroService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}