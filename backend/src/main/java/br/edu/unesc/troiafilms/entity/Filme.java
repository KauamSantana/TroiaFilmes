package br.edu.unesc.troiafilms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Título disponível no catálogo.
 *
 * <p>Mantém o lado dono do relacionamento N:N com {@link Categoria},
 * materializado na tabela associativa {@code filme_categoria}.</p>
 */
@Entity
@Table(name = "filme")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "titulo", "anoLancamento"})
public class Filme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 180)
    private String titulo;

    @Column(columnDefinition = "TEXT")
    private String sinopse;

    @Column(name = "ano_lancamento", nullable = false)
    private Integer anoLancamento;

    @Column(name = "duracao_minutos", nullable = false)
    private Integer duracaoMinutos;

    @Column(name = "poster_url", length = 500)
    private String posterUrl;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    /** Lado dono do N:N. A ordem é preservada para a resposta da API. */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "filme_categoria",
            joinColumns = @JoinColumn(name = "filme_id",
                    foreignKey = @ForeignKey(name = "fk_fc_filme")),
            inverseJoinColumns = @JoinColumn(name = "categoria_id",
                    foreignKey = @ForeignKey(name = "fk_fc_categoria"))
    )
    @Builder.Default
    private Set<Categoria> categorias = new LinkedHashSet<>();

    @OneToMany(mappedBy = "filme", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Avaliacao> avaliacoes = new ArrayList<>();

    // ------------------------------------------------- métodos de apoio N:N

    public void adicionarCategoria(Categoria categoria) {
        this.categorias.add(categoria);
        categoria.getFilmes().add(this);
    }

    public void removerCategoria(Categoria categoria) {
        this.categorias.remove(categoria);
        categoria.getFilmes().remove(this);
    }

    /** Desfaz todos os vínculos N:N antes de remover o filme. */
    public void limparCategorias() {
        this.categorias.forEach(c -> c.getFilmes().remove(this));
        this.categorias.clear();
    }
}
