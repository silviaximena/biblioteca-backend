package cl.duoc.biblioteca.service;

import cl.duoc.biblioteca.dto.AuthResponse;
import cl.duoc.biblioteca.dto.LoginRequest;
import cl.duoc.biblioteca.dto.RegistroRequest;
import cl.duoc.biblioteca.exception.CredencialesInvalidasException;
import cl.duoc.biblioteca.exception.ReglaNegocioException;
import cl.duoc.biblioteca.model.Rol;
import cl.duoc.biblioteca.model.TipoUsuario;
import cl.duoc.biblioteca.model.Usuario;
import cl.duoc.biblioteca.repository.UsuarioRepository;
import cl.duoc.biblioteca.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest datos) {
        Usuario usuario = usuarioRepository.findByCorreo(datos.correo().trim().toLowerCase())
                .filter(encontrado -> passwordEncoder.matches(datos.clave(), encontrado.getClave()))
                .orElseThrow(() -> new CredencialesInvalidasException("Correo o contraseña incorrectos"));
        return aRespuesta(usuario);
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest datos) {
        String correo = datos.correo().trim().toLowerCase();
        if (usuarioRepository.existsByCorreo(correo)) {
            throw new ReglaNegocioException("Ya existe una cuenta con el correo " + correo);
        }
        Usuario usuario = new Usuario();
        usuario.setNombreCompleto(datos.nombreCompleto().trim());
        usuario.setCorreo(correo);
        usuario.setClave(passwordEncoder.encode(datos.clave()));
        usuario.setFechaNacimiento(datos.fechaNacimiento());
        usuario.setRol(Rol.ESTUDIANTE);
        usuario.setTipoUsuario(correo.endsWith("@profesor.duoc.cl") ? TipoUsuario.PROFESOR : TipoUsuario.ESTUDIANTE);
        return aRespuesta(usuarioRepository.save(usuario));
    }

    private AuthResponse aRespuesta(Usuario usuario) {
        return new AuthResponse(
                jwtService.generarToken(usuario),
                usuario.getId(),
                usuario.getNombreCompleto(),
                usuario.getCorreo(),
                usuario.getFechaNacimiento(),
                usuario.getRol().name()
        );
    }
}