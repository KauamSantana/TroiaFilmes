package br.edu.unesc.troiafilmes.config;

import br.edu.unesc.troiafilmes.entity.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

/**
 * Gera e valida os tokens JWT, assinados com HMAC256.
 *
 * <p>A chave e a validade vêm do {@code application.properties}, e não do
 * código: assim a chave de produção nunca precisa ser commitada.</p>
 */
@Component
public class TokenConfig {

    /** Identifica quem emitiu o token. Token de outro emissor é recusado. */
    private static final String EMISSOR = "troiafilmes-api";

    private final Algorithm algoritmo;
    private final JWTVerifier verificador;
    private final long expiracaoSegundos;

    public TokenConfig(@Value("${troiafilmes.jwt.secret}") String secret,
                       @Value("${troiafilmes.jwt.expiracao-segundos}") long expiracaoSegundos) {
        this.algoritmo = Algorithm.HMAC256(secret);
        this.verificador = JWT.require(algoritmo).withIssuer(EMISSOR).build();
        this.expiracaoSegundos = expiracaoSegundos;
    }

    public String gerarToken(Usuario usuario) {
        Instant agora = Instant.now();
        return JWT.create()
                .withIssuer(EMISSOR)
                .withSubject(usuario.getEmail())
                .withClaim("usuarioId", usuario.getId())
                .withClaim("role", usuario.getRole().name())
                .withIssuedAt(agora)
                .withExpiresAt(agora.plusSeconds(expiracaoSegundos))
                .sign(algoritmo);
    }

    /**
     * Confere assinatura, emissor e validade.
     *
     * @return o e-mail do dono do token, ou vazio se o token foi adulterado,
     *         expirou ou não foi emitido por esta API
     */
    public Optional<String> validarToken(String token) {
        try {
            return Optional.of(verificador.verify(token).getSubject());
        } catch (JWTVerificationException ex) {
            return Optional.empty();
        }
    }
}
