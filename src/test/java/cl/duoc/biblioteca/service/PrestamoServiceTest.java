package cl.duoc.biblioteca.service;

import cl.duoc.biblioteca.dto.PrestamoRequest;
import cl.duoc.biblioteca.dto.PrestamoResponse;
import cl.duoc.biblioteca.exception.ReglaNegocioException;
import cl.duoc.biblioteca.model.Ejemplar;
import cl.duoc.biblioteca.model.EstadoEjemplar;
import cl.duoc.biblioteca.model.EstadoPrestamo;
import cl.duoc.biblioteca.model.Libro;
import cl.duoc.biblioteca.model.Prestamo;
import cl.duoc.biblioteca.model.Rol;
import cl.duoc.biblioteca.model.TipoUsuario;
import cl.duoc.biblioteca.model.Usuario;
import cl.duoc.biblioteca.repository.EjemplarRepository;
import cl.duoc.biblioteca.repository.LibroRepository;
import cl.duoc.biblioteca.repository.PrestamoRepository;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrestamoServiceTest {

    @Mock
    private PrestamoRepository prestamoRepository;

    @Mock
    private EjemplarRepository ejemplarRepository;

    @Mock
    private LibroRepository libroRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private PrestamoService prestamoService;

    private Usuario estudiante;
    private Libro libro;
    private Ejemplar ejemplar;

    @BeforeEach
    void setUp() {
        estudiante = new Usuario();
        estudiante.setId(3L);
        estudiante.setNombreCompleto("Estudiante Demo");
        estudiante.setCorreo("estudiante@duoc.cl");
        estudiante.setRol(Rol.ESTUDIANTE);
        estudiante.setTipoUsuario(TipoUsuario.ESTUDIANTE);

        libro = new Libro();
        libro.setId(4L);
        libro.setTitulo("Sapiens");

        ejemplar = new Ejemplar();
        ejemplar.setId(8L);
        ejemplar.setLibro(libro);
        ejemplar.setCodigo("HIS-004-A");
        ejemplar.setEstado(EstadoEjemplar.DISPONIBLE);
    }

    private void prepararUsuarioYLibro() {
        when(usuarioRepository.findByCorreo("estudiante@duoc.cl")).thenReturn(Optional.of(estudiante));
        when(libroRepository.findById(4L)).thenReturn(Optional.of(libro));
    }

    private Prestamo crearPrestamo(EstadoPrestamo estado) {
        ejemplar.setEstado(EstadoEjemplar.PRESTADO);
        Prestamo prestamo = new Prestamo();
        prestamo.setId(1L);
        prestamo.setUsuario(estudiante);
        prestamo.setEjemplar(ejemplar);
        prestamo.setFechaPrestamo(LocalDate.now());
        prestamo.setFechaVencimiento(LocalDate.now().plusDays(15));
        prestamo.setEstado(estado);
        return prestamo;
    }

    @Test
    void vencimientoDeEstudianteEsDe15Dias() {
        LocalDate lunes = LocalDate.of(2026, 11, 2);

        assertThat(PrestamoService.calcularVencimiento(TipoUsuario.ESTUDIANTE, lunes))
                .isEqualTo(LocalDate.of(2026, 11, 17));
    }

    @Test
    void vencimientoDeProfesorEsDe30Dias() {
        LocalDate lunes = LocalDate.of(2026, 11, 2);

        assertThat(PrestamoService.calcularVencimiento(TipoUsuario.PROFESOR, lunes))
                .isEqualTo(LocalDate.of(2026, 12, 2));
    }

    @Test
    void vencimientoQueCaeEnSabadoPasaAlLunes() {
        LocalDate desde = LocalDate.of(2026, 10, 30);

        assertThat(PrestamoService.calcularVencimiento(TipoUsuario.ESTUDIANTE, desde))
                .isEqualTo(LocalDate.of(2026, 11, 16));
    }

    @Test
    void vencimientoQueCaeEnDomingoPasaAlLunes() {
        LocalDate desde = LocalDate.of(2026, 10, 31);

        assertThat(PrestamoService.calcularVencimiento(TipoUsuario.ESTUDIANTE, desde))
                .isEqualTo(LocalDate.of(2026, 11, 16));
    }

    @Test
    void pedirCreaPrestamoActivoConElVencimientoDelEstudiante() {
        prepararUsuarioYLibro();
        when(prestamoRepository.countByUsuarioIdAndEstadoIn(eq(3L), anyCollection())).thenReturn(0L);
        when(prestamoRepository.existsByUsuarioIdAndEjemplarLibroIdAndEstadoIn(eq(3L), eq(4L), anyCollection())).thenReturn(false);
        when(ejemplarRepository.findFirstByLibroIdAndEstado(4L, EstadoEjemplar.DISPONIBLE)).thenReturn(Optional.of(ejemplar));
        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(invocacion -> invocacion.getArgument(0));

        PrestamoResponse respuesta = prestamoService.pedir("estudiante@duoc.cl", new PrestamoRequest(4L));

        assertThat(respuesta.estado()).isEqualTo("ACTIVO");
        assertThat(respuesta.fechaVencimiento())
                .isEqualTo(PrestamoService.calcularVencimiento(TipoUsuario.ESTUDIANTE, LocalDate.now()));
        assertThat(ejemplar.getEstado()).isEqualTo(EstadoEjemplar.PRESTADO);
    }

    @Test
    void pedirFallaSiAlcanzoElLimiteDeCincoLibros() {
        prepararUsuarioYLibro();
        when(prestamoRepository.countByUsuarioIdAndEstadoIn(eq(3L), anyCollection())).thenReturn(5L);

        assertThatThrownBy(() -> prestamoService.pedir("estudiante@duoc.cl", new PrestamoRequest(4L)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Límite de 5 libros");
        verify(prestamoRepository, never()).save(any());
    }

    @Test
    void pedirFallaSiYaTieneElMismoLibroPrestado() {
        prepararUsuarioYLibro();
        when(prestamoRepository.countByUsuarioIdAndEstadoIn(eq(3L), anyCollection())).thenReturn(1L);
        when(prestamoRepository.existsByUsuarioIdAndEjemplarLibroIdAndEstadoIn(eq(3L), eq(4L), anyCollection())).thenReturn(true);

        assertThatThrownBy(() -> prestamoService.pedir("estudiante@duoc.cl", new PrestamoRequest(4L)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("Ya tienes un préstamo activo");
    }

    @Test
    void pedirFallaSiNoQuedanEjemplaresDisponibles() {
        prepararUsuarioYLibro();
        when(prestamoRepository.countByUsuarioIdAndEstadoIn(eq(3L), anyCollection())).thenReturn(0L);
        when(prestamoRepository.existsByUsuarioIdAndEjemplarLibroIdAndEstadoIn(eq(3L), eq(4L), anyCollection())).thenReturn(false);
        when(ejemplarRepository.findFirstByLibroIdAndEstado(4L, EstadoEjemplar.DISPONIBLE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> prestamoService.pedir("estudiante@duoc.cl", new PrestamoRequest(4L)))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("No quedan ejemplares disponibles");
    }

    @Test
    void solicitarDevolucionCambiaElEstado() {
        Prestamo prestamo = crearPrestamo(EstadoPrestamo.ACTIVO);
        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));

        PrestamoResponse respuesta = prestamoService.solicitarDevolucion(1L, "estudiante@duoc.cl");

        assertThat(respuesta.estado()).isEqualTo("DEVOLUCION_SOLICITADA");
    }

    @Test
    void solicitarDevolucionDeOtroUsuarioEsRechazada() {
        Prestamo prestamo = crearPrestamo(EstadoPrestamo.ACTIVO);
        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));

        assertThatThrownBy(() -> prestamoService.solicitarDevolucion(1L, "otro@duoc.cl"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void confirmarDevolucionDejaElEjemplarDisponible() {
        Prestamo prestamo = crearPrestamo(EstadoPrestamo.DEVOLUCION_SOLICITADA);
        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));

        PrestamoResponse respuesta = prestamoService.confirmarDevolucion(1L);

        assertThat(respuesta.estado()).isEqualTo("DEVUELTO");
        assertThat(respuesta.fechaDevolucion()).isEqualTo(LocalDate.now());
        assertThat(ejemplar.getEstado()).isEqualTo(EstadoEjemplar.DISPONIBLE);
    }

    @Test
    void confirmarDevolucionDeUnPrestamoYaDevueltoFalla() {
        Prestamo prestamo = crearPrestamo(EstadoPrestamo.DEVUELTO);
        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));

        assertThatThrownBy(() -> prestamoService.confirmarDevolucion(1L))
                .isInstanceOf(ReglaNegocioException.class)
                .hasMessageContaining("ya fue devuelto");
    }
}