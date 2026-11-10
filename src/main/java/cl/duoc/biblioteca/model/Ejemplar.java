package cl.duoc.biblioteca.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "EJEMPLAR")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Ejemplar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID_EJEMPLAR")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "ID_LIBRO", nullable = false)
    private Libro libro;

    @Column(name = "CODIGO_EJEMPLAR", nullable = false, unique = true, length = 50)
    private String codigo;

    @Enumerated(EnumType.STRING)
    @Column(name = "ESTADO", nullable = false, length = 20)
    private EstadoEjemplar estado;
}