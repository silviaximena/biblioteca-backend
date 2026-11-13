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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    private Usuario crearUsuario() {
        Usuario usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombreCompleto("Mauricio Administrador");
        usuario.setCorreo("mauricio@profesor.duoc.cl");
        usuario.setClave("clave-cifrada");
        usuario.setRol(Rol.ADMINISTRADOR);
        usuario.setTipoUsuario(TipoUsuario.PROFESOR);
        return usuario;
    }

    @Test
    void loginCorrectoDevuelveTokenYRol() {
        Usuario usuario = crearUsuario();
        when(usuarioRepository.findByCorreo("mauricio@profesor.duoc.cl")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("1234", "clave-cifrada")).thenReturn(true);
        when(jwtService.generarToken(usuario)).thenReturn("token-de-prueba");

        AuthResponse respuesta = authService.login(new LoginRequest("Mauricio@profesor.duoc.cl", "1234"));

        assertThat(respuesta.token()).isEqualTo("token-de-prueba");
        assertThat(respuesta.rol()).isEqualTo("ADMINISTRADOR");
    }

    @Test
    void loginConClaveIncorrectaEsRechazado() {
        Usuario usuario = crearUsuario();
        when(usuarioRepository.findByCorreo("mauricio@profesor.duoc.cl")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("9999", "clave-cifrada")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("mauricio@profesor.duoc.cl", "9999")))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessage("Correo o contraseña incorrectos");
    }

    @Test
    void registrarCreaUnEstudianteConLaClaveCifrada() {
        when(usuarioRepository.existsByCorreo("ana@duoc.cl")).thenReturn(false);
        when(passwordEncoder.encode("1234")).thenReturn("clave-cifrada");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(jwtService.generarToken(any(Usuario.class))).thenReturn("token-de-prueba");

        AuthResponse respuesta = authService.registrar(
                new RegistroRequest("Ana Pérez", "Ana@duoc.cl", "1234", LocalDate.of(2000, 5, 20)));

        ArgumentCaptor<Usuario> guardado = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(guardado.capture());
        assertThat(guardado.getValue().getRol()).isEqualTo(Rol.ESTUDIANTE);
        assertThat(guardado.getValue().getTipoUsuario()).isEqualTo(TipoUsuario.ESTUDIANTE);
        assertThat(guardado.getValue().getClave()).isEqualTo("clave-cifrada");
        assertThat(guardado.getValue().getCorreo()).isEqualTo("ana@duoc.cl");
        assertThat(respuesta.rol()).isEqualTo("ESTUDIANTE");
    }

    @Test
    void registrarConCorreoRepetidoFalla() {
        when(usuarioRepository.existsByCorreo("estudiante@duoc.cl")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(
                new RegistroRequest("Otro", "estudiante@duoc.cl", "1234", LocalDate.of(2001, 1, 1))))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Ya existe una cuenta");
    }
}