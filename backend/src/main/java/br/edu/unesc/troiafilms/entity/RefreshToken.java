package br.edu.unesc.troiafilms.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Refresh token emitido no login.
 *
 * <p>É persistido, e não apenas assinado, para que o logout consiga
 * revogá-lo de fato: um JWT isolado não tem como ser invalidado antes de
 * expirar.</p>
 */
@Entity
@Table(name = "refresh_token")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "expiraEm", "revogado"})
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_refresh_token_usuario"))
    private Usuario usuario;

    @Column(nullable = false, unique = true, length = 255)
    private String token;

    @Column(name = "expira_em", nullable = false)
    private LocalDateTime expiraEm;

    @Column(nullable = false)
    @Builder.Default
    private Boolean revogado = false;

    @CreationTimestamp
    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    public boolean estaExpirado() {
        return expiraEm.isBefore(LocalDateTime.now());
    }

    /** Um token só serve se não foi revogado e ainda está no prazo. */
    public boolean estaValido() {
        return Boolean.FALSE.equals(revogado) && !estaExpirado();
    }
}
