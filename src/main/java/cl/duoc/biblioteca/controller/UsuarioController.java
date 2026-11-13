package cl.duoc.biblioteca.controller;

import cl.duoc.biblioteca.dto.PerfilRequest;
import cl.duoc.biblioteca.dto.UsuarioResponse;
import cl.duoc.biblioteca.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/usuarios")
@RequiredArgsConstructor
@Tag(name = "Usuarios", description = "Usuarios registrados y perfil propio")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @Operation(summary = "Listar usuarios registrados (Bibliotecario y Administrador)")
    public List<UsuarioResponse> listar() {
        return usuarioService.listar();
    }

    @GetMapping("/yo")
    @Operation(summary = "Ver mi perfil (usuario con sesión)")
    public UsuarioResponse verPerfil(Authentication autenticacion) {
        return usuarioService.verPerfil(autenticacion.getName());
    }

    @PutMapping("/yo")
    @Operation(summary = "Editar mi nombre y fecha de nacimiento (usuario con sesión)")
    public UsuarioResponse actualizarPerfil(@Valid @RequestBody PerfilRequest datos, Authentication autenticacion) {
        return usuarioService.actualizarPerfil(autenticacion.getName(), datos);
    }
}