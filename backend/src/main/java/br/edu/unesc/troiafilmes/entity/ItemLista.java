package br.edu.unesc.troiafilmes.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Item da "Minha Lista" de um perfil.
 *
 * <p>É a entidade associativa que resolve o segundo relacionamento N:N do
 * sistema (Perfil x Filme). Diferente de uma tabela de junção pura, carrega
 * atributo próprio ({@code adicionadoEm}), por isso é modelada como
 * entidade.</p>
 */
@Entity
@Table(
        name = "item_lista",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_item_lista_perfil_filme",
                columnNames = {"perfil_id", "filme_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = "id")
public class ItemLista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perfil_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_item_lista_perfil"))
    private Perfil perfil;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "filme_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_item_lista_filme"))
    private Filme filme;

    @CreationTimestamp
    @Column(name = "adicionado_em", nullable = false, updatable = false)
    private LocalDateTime adicionadoEm;
}
