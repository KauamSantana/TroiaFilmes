package br.edu.unesc.troiafilms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Nota e comentário de um perfil sobre um filme.
 *
 * <p>Regra de negócio: um perfil avalia cada filme uma única vez — garantido
 * pela constraint {@code uk_avaliacao_perfil_filme} e validado no service.</p>
 */
@Entity
@Table(
        name = "avaliacao",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_avaliacao_perfil_filme",
                columnNames = {"perfil_id", "filme_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "nota"})
public class Avaliacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perfil_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_avaliacao_perfil"))
    private Perfil perfil;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "filme_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_avaliacao_filme"))
    private Filme filme;

    /** Escala de 1 a 5. */
    @Column(nullable = false)
    private Integer nota;

    @Column(columnDefinition = "TEXT")
    private String comentario;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;
}
