package br.edu.unesc.troiafilms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Perfil de consumo dentro de uma conta, no mesmo modelo dos serviços de
 * streaming: a conta é do usuário, mas o histórico e as avaliações são
 * por perfil.
 *
 * <p>A quantidade de perfis é limitada pelo plano da assinatura ativa.</p>
 */
@Entity
@Table(
        name = "perfil",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_perfil_usuario_nome",
                columnNames = {"usuario_id", "nome"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "nome", "infantil"})
public class Perfil {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_perfil_usuario"))
    private Usuario usuario;

    @Column(nullable = false, length = 60)
    private String nome;

    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean infantil = false;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Avaliacao> avaliacoes = new ArrayList<>();

    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ItemLista> itensDaLista = new ArrayList<>();

    @OneToMany(mappedBy = "perfil", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<HistoricoVisualizacao> historico = new ArrayList<>();
}
