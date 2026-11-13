package cl.duoc.biblioteca.service;

import cl.duoc.biblioteca.dto.ReservaRequest;
import cl.duoc.biblioteca.dto.ReservaResponse;
import cl.duoc.biblioteca.exception.RecursoNoEncontradoException;
import cl.duoc.biblioteca.exception.ReglaNegocioException;
import cl.duoc.biblioteca.model.EstadoEjemplar;
import cl.duoc.biblioteca.model.EstadoReserva;
import cl.duoc.biblioteca.model.Libro;
import cl.duoc.biblioteca.model.Reserva;
import cl.duoc.biblioteca.model.Usuario;
import cl.duoc.biblioteca.repository.EjemplarRepository;
import cl.duoc.biblioteca.repository.LibroRepository;
import cl.duoc.biblioteca.repository.ReservaRepository;
import cl.duoc.biblioteca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReservaService {

    private static final int DIAS_VIGENCIA = 7;

    private final ReservaRepository reservaRepository;
    private final LibroRepository libroRepository;
    private final EjemplarRepository ejemplarRepository;
    private final UsuarioRepository usuarioRepository;

    @Transactional
    public ReservaResponse reservar(String correo, ReservaRequest datos) {
        Usuario usuario = usuarioRepository.findByCorreo(correo)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + correo));
        Libro libro = libroRepository.findById(datos.libroId())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un libro con id " + datos.libroId()));

        if (ejemplarRepository.countByLibroIdAndEstado(libro.getId(), EstadoEjemplar.DISPONIBLE) > 0) {
            throw new ReglaNegocioException("Hay ejemplares disponibles de " + libro.getTitulo() + ": puedes pedir el préstamo directamente");
        }
        if (reservaRepository.existsByUsuarioIdAndLibroIdAndEstado(usuario.getId(), libro.getId(), EstadoReserva.VIGENTE)) {
            throw new ReglaNegocioException("Ya tienes una reserva vigente de " + libro.getTitulo());
        }

        Reserva reserva = new Reserva();
        reserva.setUsuario(usuario);
        reserva.setLibro(libro);
        reserva.setFechaReserva(LocalDate.now());
        reserva.setFechaVigencia(LocalDate.now().plusDays(DIAS_VIGENCIA));
        reserva.setEstado(EstadoReserva.VIGENTE);
        return aRespuesta(reservaRepository.save(reserva));
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> listarTodas() {
        return reservaRepository.findAll(Sort.by(Sort.Direction.DESC, "id")).stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReservaResponse> listarMias(String correo) {
        return reservaRepository.findByUsuarioCorreoOrderByIdDesc(correo).stream()
                .map(this::aRespuesta)
                .toList();
    }

    @Transactional
    public ReservaResponse cancelar(Long id, String correo) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe una reserva con id " + id));
        if (!reserva.getUsuario().getCorreo().equals(correo)) {
            throw new AccessDeniedException("Solo el dueño de la reserva puede cancelarla");
        }
        if (reserva.getEstado() != EstadoReserva.VIGENTE) {
            throw new ReglaNegocioException("Solo se puede cancelar una reserva vigente");
        }
        reserva.setEstado(EstadoReserva.CANCELADA);
        return aRespuesta(reserva);
    }

    private ReservaResponse aRespuesta(Reserva reserva) {
        String estado = reserva.getEstado() == EstadoReserva.VIGENTE && reserva.getFechaVigencia().isBefore(LocalDate.now())
                ? EstadoReserva.VENCIDA.name()
                : reserva.getEstado().name();
        return new ReservaResponse(
                reserva.getId(),
                reserva.getUsuario().getId(),
                reserva.getUsuario().getNombreCompleto(),
                reserva.getUsuario().getCorreo(),
                reserva.getLibro().getId(),
                reserva.getLibro().getTitulo(),
                reserva.getFechaReserva(),
                reserva.getFechaVigencia(),
                estado
        );
    }
}