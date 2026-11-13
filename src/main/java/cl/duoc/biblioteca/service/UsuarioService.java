package cl.duoc.biblioteca.service;

import cl.duoc.biblioteca.dto.PerfilRequest;
import cl.duoc.biblioteca.dto.UsuarioResponse;
import cl.duoc.biblioteca.exception.RecursoNoEncontradoException;
import cl.duoc.biblioteca.model.Usuario;
import cl.duoc.biblioteca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<UsuarioResponse> listar() {
        return usuarioRepository.findAll(Sort.by("id")).stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public UsuarioResponse verPerfil(String correo) {
        return aRespuesta(obtenerUsuario(correo));
    }

    @Transactional
    public UsuarioResponse actualizarPerfil(String correo, PerfilRequest datos) {
        Usuario usuario = obtenerUsuario(correo);
        usuario.setNombreCompleto(datos.nombreCompleto().trim());
        usuario.setFechaNacimiento(datos.fechaNacimiento());
        return aRespuesta(usuario);
    }

    private Usuario obtenerUsuario(String correo) {
        return usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + correo));
    }

    private UsuarioResponse aRespuesta(Usuario usuario) {
        String categoria = usuario.getCorreo().endsWith("@profesor.duoc.cl") ? "Docente" : "Estudiante";
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getCorreo(),
                usuario.getFechaNacimiento(),
                usuario.getTipoUsuario().name(),
                categoria
        );
    }
}
