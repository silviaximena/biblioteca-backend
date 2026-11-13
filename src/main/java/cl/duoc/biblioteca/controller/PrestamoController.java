package cl.duoc.biblioteca.controller;

import cl.duoc.biblioteca.dto.PrestamoRequest;
import cl.duoc.biblioteca.dto.PrestamoResponse;
import cl.duoc.biblioteca.service.PrestamoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/prestamos")
@RequiredArgsConstructor
@Tag(name = "Préstamos", description = "Préstamos y devoluciones de libros")
public class PrestamoController {

    private final PrestamoService prestamoService;

    @PostMapping
    @Operation(summary = "Pedir un libro prestado (usuario con sesión)")
    public ResponseEntity<PrestamoResponse> pedir(@Valid @RequestBody PrestamoRequest datos, Authentication autenticacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(prestamoService.pedir(autenticacion.getName(), datos));
    }

    @GetMapping("/mios")
    @Operation(summary = "Ver mis préstamos (usuario con sesión)")
    public List<PrestamoResponse> listarMios(Authentication autenticacion) {
        return prestamoService.listarMios(autenticacion.getName());
    }

    @GetMapping
    @Operation(summary = "Ver todos los préstamos (Bibliotecario y Administrador)")
    public List<PrestamoResponse> listarTodos() {
        return prestamoService.listarTodos();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Ver el detalle de un préstamo (Bibliotecario y Administrador)")
    public PrestamoResponse buscarPorId(@PathVariable Long id) {
        return prestamoService.buscarPorId(id);
    }

    @PutMapping("/{id}/solicitar-devolucion")
    @Operation(summary = "Solicitar la devolución de un préstamo propio")
    public PrestamoResponse solicitarDevolucion(@PathVariable Long id, Authentication autenticacion) {
        return prestamoService.solicitarDevolucion(id, autenticacion.getName());
    }

    @PutMapping("/{id}/confirmar-devolucion")
    @Operation(summary = "Confirmar la devolución de un préstamo (Bibliotecario y Administrador)")
    public PrestamoResponse confirmarDevolucion(@PathVariable Long id) {
        return prestamoService.confirmarDevolucion(id);
    }
}