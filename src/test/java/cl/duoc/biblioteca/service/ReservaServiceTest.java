package cl.duoc.biblioteca.service;

import cl.duoc.biblioteca.dto.ReservaRequest;
import cl.duoc.biblioteca.dto.ReservaResponse;
import cl.duoc.biblioteca.exception.ReglaNegocioException;
import cl.duoc.biblioteca.model.EstadoEjemplar;
import cl.duoc.biblioteca.model.EstadoReserva;
import cl.duoc.biblioteca.model.Libro;
import cl.duoc.biblioteca.model.Reserva;
import cl.duoc.biblioteca.model.Rol;
import cl.duoc.biblioteca.model.TipoUsuario;
import cl.duoc.biblioteca.model.Usuario;
import cl.duoc.biblioteca.repository.EjemplarRepository;
import cl.duoc.biblioteca.repository.LibroRepository;
import cl.duoc.biblioteca.repository.ReservaRepository;
import cl.duoc.biblioteca.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private LibroRepository libroRepository;

    @Mock
    private EjemplarRepository ejemplarRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ReservaService reservaService;

    private Usuario usuario;
    private Libro libro;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(2L);
        usuario.setNombreCompleto("Biblioteca Duoc");
        usuario.setCorreo("biblioteca@duoc.cl");
        usuario.setRol(Rol.BIBLIOTECARIO);
        usuario.setTipoUsuario(TipoUsuario.ESTUDIANTE);

        libro = new Libro();
        libro.setId(4L);
        libro.setTitulo("Sapiens");
    }

    private void prepararUsuarioYLibro() {
        when(usuarioRepository.findByCorreo("biblioteca@duoc.cl")).thenReturn(Optional.of(usuario));
        when(libroRepository.findById(4L)).thenReturn(Optional.of(libro));
    }

    @Test
    void reservarCreaReservaVigentePorSieteDias() {
        prepararUsuarioYLibro();
        when(ejemplarRepository.countByLibroIdAndEstado(4L, EstadoEjemplar.DISPONIBLE)).thenReturn(0L);
        when(reservaRepository.existsByUsuarioIdAndLibroIdAndEstado(2L, 4L, EstadoReserva.VIGENTE)).thenReturn(false);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        ReservaResponse respuesta = reservaService.reservar("biblioteca@duoc.cl", new ReservaRequest(4L));

        assertThat(respuesta.estado()).isEqualTo("VIGENTE");
        assertThat(respuesta.fechaVigencia()).isEqualTo(LocalDate.now().plusDays(7));
    }

    @Test
    void reservarFallaSiHayEjemplaresDisponibles() {
        prepararUsuarioYLibro();
        when(ejemplarRepository.countByLibroIdAndEstado(4L, EstadoEjemplar.DISPONIBLE)).thenReturn(2L);

        assertThatThrownBy(() -> reservaService.reservar("biblioteca@duoc.cl", new ReservaRequest(4L)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("puedes pedir el préstamo");
        verify(reservaRepository, never()).save(any());
    }

    @Test
    void reservarFallaSiYaTieneUnaReservaVigente() {
        prepararUsuarioYLibro();
        when(ejemplarRepository.countByLibroIdAndEstado(4L, EstadoEjemplar.DISPONIBLE)).thenReturn(0L);
        when(reservaRepository.existsByUsuarioIdAndLibroIdAndEstado(2L, 4L, EstadoReserva.VIGENTE)).thenReturn(true);

        assertThatThrownBy(() -> reservaService.reservar("biblioteca@duoc.cl", new ReservaRequest(4L)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Ya tienes una reserva vigente");
    }

    @Test
    void cancelarReservaDeOtroUsuarioEsRechazada() {
        Reserva reserva = new Reserva();
        reserva.setId(1L);
        reserva.setUsuario(usuario);
        reserva.setLibro(libro);
        reserva.setFechaReserva(LocalDate.now());
        reserva.setFechaVigencia(LocalDate.now().plusDays(7));
        reserva.setEstado(EstadoReserva.VIGENTE);
        when(reservaRepository.findById(1L)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> reservaService.cancelar(1L, "otro@duoc.cl"))
                .isInstanceOf(AccessDeniedException.class);
    }
}