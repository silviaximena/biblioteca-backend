package cl.duoc.biblioteca.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "LIBRO")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Libro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_LIBRO")
    private Long id;

    @Column(name = "TITULO", nullable = false, length = 150)
    private String titulo;

    @Column(name = "ISBN", unique = true, length = 20)
    private String isbn;

    @Column(name = "ANIO")
    private Integer anio;

    @Column(name = "PORTADA", length = 500)
    private String portada;

    @ManyToOne
    @JoinColumn(name = "ID_EDITORIAL")
    private Editorial editorial;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ID_CATEGORIA", nullable = false)
    private Categoria categoria;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "LIBRO_AUTOR",
            joinColumns = @JoinColumn(name = "ID_LIBRO"),
            inverseJoinColumns = @JoinColumn(name = "ID_AUTOR")
    )
    private Set<Autor> autores = new HashSet<>();
}