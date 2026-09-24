package br.edu.unesc.troiafilms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Progresso de reprodução de um filme por um perfil, base do
 * "Continuar assistindo".
 *
 * <p>Regra de negócio: o registro é marcado como concluído quando o
 * progresso atinge 90% da duração do filme.</p>
 */
@Entity
@Table(
        name = "historico_visualizacao",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_historico_perfil_filme",
                columnNames = {"perfil_id", "filme_id"})
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "minutosAssistidos", "concluido"})
public class HistoricoVisualizacao {

    /** Fração da duração a partir da qual o filme é considerado assistido. */
    public static final double FRACAO_PARA_CONCLUIR = 0.9;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "perfil_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_historico_perfil"))
    private Perfil perfil;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "filme_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_historico_filme"))
    private Filme filme;

    @Column(name = "minutos_assistidos", nullable = false)
    @Builder.Default
    private Integer minutosAssistidos = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean concluido = false;

    @Column(name = "assistido_em", nullable = false)
    @Builder.Default
    private LocalDateTime assistidoEm = LocalDateTime.now();

    /**
     * Registra o progresso e reavalia a conclusão.
     *
     * @param minutos      posição atual de reprodução, em minutos
     * @param duracaoFilme duração total do filme, em minutos
     */
    public void registrarProgresso(int minutos, int duracaoFilme) {
        this.minutosAssistidos = minutos;
        this.concluido = minutos >= duracaoFilme * FRACAO_PARA_CONCLUIR;
        this.assistidoEm = LocalDateTime.now();
    }

    /** Percentual assistido, de 0 a 100, para a barra de progresso da UI. */
    public int getPercentualAssistido() {
        int duracao = filme != null && filme.getDuracaoMinutos() != null
                ? filme.getDuracaoMinutos() : 0;
        if (duracao <= 0) {
            return 0;
        }
        return Math.min(100, (int) Math.round(minutosAssistidos * 100.0 / duracao));
    }
}
