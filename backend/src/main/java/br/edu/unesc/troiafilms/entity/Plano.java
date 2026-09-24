package br.edu.unesc.troiafilms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Plano de assinatura comercializado pelo TroiaFilms.
 *
 * <p>O campo {@code maxPerfis} é o que sustenta a regra de negócio de limite
 * de perfis por conta.</p>
 */
@Entity
@Table(name = "plano")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "nome", "preco"})
public class Plano {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String nome;

    @Column(length = 255)
    private String descricao;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal preco;

    /** Quantidade máxima de perfis que o assinante pode manter. */
    @Column(name = "max_perfis", nullable = false)
    private Integer maxPerfis;

    @Enumerated(EnumType.STRING)
    @Column(name = "qualidade_maxima", nullable = false, length = 10)
    private Qualidade qualidadeMaxima;

    @Column(nullable = false)
    @Builder.Default
    private Boolean ativo = true;

    @OneToMany(mappedBy = "plano")
    @Builder.Default
    private List<Assinatura> assinaturas = new ArrayList<>();
}
