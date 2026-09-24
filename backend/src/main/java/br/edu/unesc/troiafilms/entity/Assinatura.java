package br.edu.unesc.troiafilms.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Vínculo entre um usuário e um plano, preservando o histórico de trocas.
 *
 * <p>Regra de negócio: um usuário tem no máximo uma assinatura
 * {@link StatusAssinatura#ATIVA}. Ao contratar um novo plano, a anterior é
 * automaticamente cancelada. A restrição também existe no banco, como
 * índice único parcial ({@code uk_assinatura_ativa_por_usuario}).</p>
 */
@Entity
@Table(name = "assinatura")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "status", "dataInicio", "dataFim"})
public class Assinatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_assinatura_usuario"))
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plano_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_assinatura_plano"))
    private Plano plano;

    @Column(name = "data_inicio", nullable = false)
    @Builder.Default
    private LocalDate dataInicio = LocalDate.now();

    @Column(name = "data_fim")
    private LocalDate dataFim;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private StatusAssinatura status = StatusAssinatura.ATIVA;

    public boolean estaAtiva() {
        return StatusAssinatura.ATIVA.equals(this.status);
    }

    /** Encerra a assinatura registrando a data de término. */
    public void cancelar() {
        this.status = StatusAssinatura.CANCELADA;
        this.dataFim = LocalDate.now();
    }
}
