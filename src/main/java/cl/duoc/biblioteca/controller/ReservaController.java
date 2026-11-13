package cl.duoc.biblioteca.controller;

import cl.duoc.biblioteca.dto.ReservaRequest;
import cl.duoc.biblioteca.dto.ReservaResponse;
import cl.duoc.biblioteca.service.ReservaService;
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
@RequestMapping("/api/v1/reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "Reservas de libros sin ejemplares disponibles")
public class ReservaController {

    private final ReservaService reservaService;

    @PostMapping
    @Operation(summary = "Reservar un libro agotado (usuario con sesión)")
    public ResponseEntity<ReservaResponse> reservar(@Valid @RequestBody ReservaRequest datos, Authentication autenticacion) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reservaService.reservar(autenticacion.getName(), datos));
    }

    @GetMapping("/mias")
    @Operation(summary = "Ver mis reservas (usuario con sesión)")
    public List<ReservaResponse> listarMias(Authentication autenticacion) {
        return reservaService.listarMias(autenticacion.getName());
    }

    @GetMapping
    @Operation(summary = "Ver todas las reservas (Bibliotecario y Administrador)")
    public List<ReservaResponse> listarTodas() {
        return reservaService.listarTodas();
    }

    @PutMapping("/{id}/cancelar")
    @Operation(summary = "Cancelar una reserva propia")
    public ReservaResponse cancelar(@PathVariable Long id, Authentication autenticacion) {
        return reservaService.cancelar(id, autenticacion.getName());
    }
}