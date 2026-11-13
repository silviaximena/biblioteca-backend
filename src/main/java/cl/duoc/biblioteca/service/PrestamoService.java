package cl.duoc.biblioteca.service;

import cl.duoc.biblioteca.dto.PrestamoRequest;
import cl.duoc.biblioteca.dto.PrestamoResponse;
import cl.duoc.biblioteca.exception.RecursoNoEncontradoException;
import cl.duoc.biblioteca.exception.ReglaNegocioException;
import cl.duoc.biblioteca.model.Ejemplar;
import cl.duoc.biblioteca.model.EstadoEjemplar;
import cl.duoc.biblioteca.model.EstadoPrestamo;
import cl.duoc.biblioteca.model.Libro;
import cl.duoc.biblioteca.model.Prestamo;
import cl.duoc.biblioteca.model.TipoUsuario;
import cl.duoc.biblioteca.model.Usuario;
import cl.duoc.biblioteca.repository.EjemplarRepository;
import cl.duoc.biblioteca.repository.LibroRepository;
import cl.duoc.biblioteca.repository.PrestamoRepository;
import cl.duoc.biblioteca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PrestamoService {

    private static final int LIMITE_PRESTAMOS = 5;
    private static final int DIAS_ESTUDIANTE = 15;
    private static final int DIAS_PROFESOR = 30;
    private static final List<EstadoPrestamo> ESTADOS_ACTIVOS =
            List.of(EstadoPrestamo.ACTIVO, EstadoPrestamo.DEVOLUCION_SOLICITADA, EstadoPrestamo.ATRASADO);

    private final PrestamoRepository prestamoRepository;
    private final EjemplarRepository ejemplarRepository;
    private final LibroRepository libroRepository;
    private final UsuarioRepository usuarioRepository;

    public static LocalDate calcularVencimiento(TipoUsuario tipoUsuario, LocalDate desde) {
        int dias = tipoUsuario == TipoUsuario.PROFESOR ? DIAS_PROFESOR : DIAS_ESTUDIANTE;
        LocalDate vencimiento = desde.plusDays(dias);
        if (vencimiento.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return vencimiento.plusDays(2);
        }
        if (vencimiento.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return vencimiento.plusDays(1);
        }
        return vencimiento;
    }

    @Transactional
    public PrestamoResponse pedir(String correo, PrestamoRequest datos) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + correo));
        Libro libro = libroRepository.findById(datos.libroId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un libro con id " + datos.libroId()));

        if (prestamoRepository.countByUsuarioIdAndEstadoIn(usuario.getId(), ESTADOS_ACTIVOS) >= LIMITE_PRESTAMOS) {
            throw new ReglaNegocioException("Límite de " + LIMITE_PRESTAMOS + " libros alcanzado. Devuelve uno para pedir otro");
        }
        if (prestamoRepository.existsByUsuarioIdAndEjemplarLibroIdAndEstadoIn(usuario.getId(), libro.getId(), ESTADOS_ACTIVOS)) {
            throw new ReglaNegocioException("Ya tienes un préstamo activo de " + libro.getTitulo());
        }

        Ejemplar ejemplar = ejemplarRepository.findFirstByLibroIdAndEstado(libro.getId(), EstadoEjemplar.DISPONIBLE)
                .orElseThrow(() -> new ReglaNegocioException("No quedan ejemplares disponibles de " + libro.getTitulo()));
        ejemplar.setEstado(EstadoEjemplar.PRESTADO);

        Prestamo prestamo = new Prestamo();
        prestamo.setUsuario(usuario);
        prestamo.setEjemplar(ejemplar);
        prestamo.setFechaPrestamo(LocalDate.now());
        prestamo.setFechaVencimiento(calcularVencimiento(usuario.getTipoUsuario(), LocalDate.now()));
        prestamo.setEstado(EstadoPrestamo.ACTIVO);
        return aRespuesta(prestamoRepository.save(prestamo));
    }

    @Transactional(readOnly = true)
    public List<PrestamoResponse> listarTodos() {
        return prestamoRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PrestamoResponse> listarMios(String correo) {
        return prestamoRepository.findByUsuarioCorreoOrderByIdDesc(correo).stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public PrestamoResponse buscarPorId(Long id) {
        return aRespuesta(obtenerPrestamo(id));
    }

    @Transactional
    public PrestamoResponse solicitarDevolucion(Long id, String correo) {
        Prestamo prestamo = obtenerPrestamo(id);
        if (!prestamo.getUsuario().getCorreo().equals(correo)) {
            throw new AccessDeniedException("Solo el dueño del préstamo puede solicitar su devolución");
        }
        if (prestamo.getEstado() != EstadoPrestamo.ACTIVO && prestamo.getEstado() != EstadoPrestamo.ATRASADO) {
            throw new ReglaNegocioException("Solo se puede solicitar la devolución de un préstamo activo");
        }
        prestamo.setEstado(EstadoPrestamo.DEVOLUCION_SOLICITADA);
        return aRespuesta(prestamo);
    }

    @Transactional
    public PrestamoResponse confirmarDevolucion(Long id) {
        Prestamo prestamo = obtenerPrestamo(id);
        if (prestamo.getEstado() == EstadoPrestamo.DEVUELTO) {
            throw new ReglaNegocioException("Este préstamo ya fue devuelto");
        }
        prestamo.setEstado(EstadoPrestamo.DEVUELTO);
        prestamo.setFechaDevolucion(LocalDate.now());
        prestamo.getEjemplar().setEstado(EstadoEjemplar.DISPONIBLE);
        return aRespuesta(prestamo);
    }

    private Prestamo obtenerPrestamo(Long id) {
        return prestamoRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un préstamo con id " + id));
    }

    private PrestamoResponse aRespuesta(Prestamo prestamo) {
        boolean atrasado = prestamo.getEstado() == EstadoPrestamo.ATRASADO
                || (prestamo.getEstado() != EstadoPrestamo.DEVUELTO
                && prestamo.getFechaVencimiento().isBefore(LocalDate.now()));
        return new PrestamoResponse(
                prestamo.getId(),
                prestamo.getUsuario().getId(),
                prestamo.getUsuario().getNombreCompleto(),
                prestamo.getUsuario().getCorreo(),
                prestamo.getEjemplar().getLibro().getId(),
                prestamo.getEjemplar().getLibro().getTitulo(),
                prestamo.getEjemplar().getCodigo(),
                prestamo.getFechaPrestamo(),
                prestamo.getFechaVencimiento(),
                prestamo.getFechaDevolucion(),
                prestamo.getEstado().name(),
                atrasado
        );
    }
}