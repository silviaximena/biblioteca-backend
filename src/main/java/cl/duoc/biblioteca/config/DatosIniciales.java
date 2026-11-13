
package cl.duoc.biblioteca.config;

import cl.duoc.biblioteca.model.TipoUsuario;
import cl.duoc.biblioteca.model.Usuario;
import cl.duoc.biblioteca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DatosIniciales implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        crearSiNoExiste("Mauricio Administrador", "mauricio@profesor.duoc.cl", TipoUsuario.ADMINISTRADOR);
        crearSiNoExiste("Biblioteca Duoc", "biblioteca@duoc.cl", TipoUsuario.BIBLIOTECARIO);
        crearSiNoExiste("Estudiante Demo", "estudiante@duoc.cl", TipoUsuario.ESTUDIANTE);
    }

    private void crearSiNoExiste(String nombre, String correo, TipoUsuario tipo) {
        if (!usuarioRepository.existsByCorreo(correo)) {
            Usuario usuario = new Usuario();
            usuario.setNombreCompleto(nombre);
            usuario.setCorreo(correo);
            usuario.setClave(passwordEncoder.encode("1234"));
            usuario.setTipoUsuario(tipo);
            usuarioRepository.save(usuario);
        }
    }
}