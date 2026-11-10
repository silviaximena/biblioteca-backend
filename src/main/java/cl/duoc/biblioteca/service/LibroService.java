package cl.duoc.biblioteca.service;

import cl.duoc.biblioteca.dto.LibroRequest;
import cl.duoc.biblioteca.dto.LibroResponse;
import cl.duoc.biblioteca.exception.RecursoNoEncontradoException;
import cl.duoc.biblioteca.exception.ReglaNegocioException;
import cl.duoc.biblioteca.model.Autor;
import cl.duoc.biblioteca.model.Categoria;
import cl.duoc.biblioteca.model.Editorial;
import cl.duoc.biblioteca.model.Ejemplar;
import cl.duoc.biblioteca.model.EstadoEjemplar;
import cl.duoc.biblioteca.model.Libro;
import cl.duoc.biblioteca.repository.AutorRepository;
import cl.duoc.biblioteca.repository.CategoriaRepository;
import cl.duoc.biblioteca.repository.EditorialRepository;
import cl.duoc.biblioteca.repository.EjemplarRepository;
import cl.duoc.biblioteca.repository.LibroRepository;
import cl.duoc.biblioteca.repository.PrestamoRepository;
import cl.duoc.biblioteca.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LibroService {

    private final LibroRepository libroRepository;
    private final EjemplarRepository ejemplarRepository;
    private final CategoriaRepository categoriaRepository;
    private final EditorialRepository editorialRepository;
    private final AutorRepository autorRepository;
    private final PrestamoRepository prestamoRepository;
    private final ReservaRepository reservaRepository;

    @Transactional(readOnly = true)
    public List<LibroResponse> listar(String categoria, String texto) {
        List<Libro> libros;
        if (categoria != null && !categoria.isBlank()) {
            libros = libroRepository.findByCategoriaNombreIgnoreCase(categoria.trim());
        } else if (texto != null && !texto.isBlank()) {
            libros = libroRepository.findByTituloContainingIgnoreCase(texto.trim());
        } else {
            libros = libroRepository.findAll(Sort.by("id"));
        }
        return libros.stream().map(this::aRespuesta).toList();
    }

    @Transactional(readOnly = true)
    public LibroResponse buscarPorId(Long id) {
        return aRespuesta(obtenerLibro(id));
    }

    @Transactional
    public LibroResponse crear(LibroRequest datos) {
        Libro libro = new Libro();
        aplicarDatos(libro, datos);
        Libro guardado = libroRepository.save(libro);
        agregarEjemplares(guardado, datos.stock());
        return aRespuesta(guardado);
    }

    @Transactional
    public LibroResponse actualizar(Long id, LibroRequest datos) {
        Libro libro = obtenerLibro(id);
        aplicarDatos(libro, datos);
        Libro guardado = libroRepository.save(libro);
        ajustarStock(guardado, datos.stock());
        return aRespuesta(guardado);
    }

    @Transactional
    public void eliminar(Long id) {
        Libro libro = obtenerLibro(id);
        if (prestamoRepository.existsByEjemplarLibroId(id) || reservaRepository.existsByLibroId(id)) {
            throw new ReglaNegocioException("No se puede eliminar el libro porque tiene préstamos o reservas registradas");
        }
        ejemplarRepository.deleteAll(ejemplarRepository.findByLibroId(id));
        libroRepository.delete(libro);
    }

    private Libro obtenerLibro(Long id) {
        return libroRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe un libro con id " + id));
    }

    private void aplicarDatos(Libro libro, LibroRequest datos) {
        Categoria categoria = categoriaRepository.findByNombreIgnoreCase(datos.categoria().trim())
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe la categoría " + datos.categoria()));
        libro.setTitulo(datos.titulo().trim());
        libro.setIsbn(textoONulo(datos.isbn()));
        libro.setAnio(datos.anio());
        libro.setPortada(textoONulo(datos.portada()));
        libro.setCategoria(categoria);
        libro.setEditorial(obtenerEditorial(datos.editorial()));
        libro.setAutores(obtenerAutores(datos.autor()));
    }

    private Editorial obtenerEditorial(String nombre) {
        String limpio = textoONulo(nombre);
        if (limpio == null) {
            return null;
        }
        return editorialRepository.findByNombreIgnoreCase(limpio)
                .orElseGet(() -> editorialRepository.save(new Editorial(null, limpio)));
    }

    private Set<Autor> obtenerAutores(String texto) {
        return Arrays.stream(texto.split(","))
                .map(String::trim)
                .filter(nombre -> !nombre.isEmpty())
                .map(nombre -> autorRepository.findByNombreIgnoreCase(nombre)
                        .orElseGet(() -> autorRepository.save(new Autor(null, nombre))))
                .collect(Collectors.toSet());
    }

    private void agregarEjemplares(Libro libro, int cantidad) {
        for (int i = 0; i < cantidad; i++) {
            Ejemplar ejemplar = new Ejemplar();
            ejemplar.setLibro(libro);
            ejemplar.setCodigo("L" + libro.getId() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
            ejemplar.setEstado(EstadoEjemplar.DISPONIBLE);
            ejemplarRepository.save(ejemplar);
        }
    }

    private void ajustarStock(Libro libro, int stockNuevo) {
        long stockActual = ejemplarRepository.countByLibroId(libro.getId());
        if (stockNuevo > stockActual) {
            agregarEjemplares(libro, (int) (stockNuevo - stockActual));
        } else if (stockNuevo < stockActual) {
            long porQuitar = stockActual - stockNuevo;
            List<Ejemplar> quitables = ejemplarRepository.findByLibroIdAndEstado(libro.getId(), EstadoEjemplar.DISPONIBLE)
                    .stream()
                    .filter(ejemplar -> !prestamoRepository.existsByEjemplarId(ejemplar.getId()))
                    .limit(porQuitar)
                    .toList();
            if (quitables.size() < porQuitar) {
                throw new ReglaNegocioException("No se puede bajar el stock a " + stockNuevo + ": hay ejemplares prestados o con historial de préstamos");
            }
            ejemplarRepository.deleteAll(quitables);
        }
    }

    private String textoONulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private LibroResponse aRespuesta(Libro libro) {
        String autores = libro.getAutores().stream()
                .map(Autor::getNombre)
                .sorted()
                .collect(Collectors.joining(", "));

        return new LibroResponse(
                libro.getId(),
                libro.getTitulo(),
                autores,
                libro.getIsbn(),
                libro.getAnio(),
                libro.getPortada(),
                libro.getCategoria().getNombre(),
                libro.getEditorial() != null ? libro.getEditorial().getNombre() : null,
                ejemplarRepository.countByLibroId(libro.getId()),
                ejemplarRepository.countByLibroIdAndEstado(libro.getId(), EstadoEjemplar.DISPONIBLE)
        );
    }
}