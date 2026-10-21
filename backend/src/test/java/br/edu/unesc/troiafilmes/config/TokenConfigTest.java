package br.edu.unesc.troiafilmes.config;

import br.edu.unesc.troiafilmes.entity.Role;
import br.edu.unesc.troiafilmes.entity.Usuario;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("TokenConfig")
class TokenConfigTest {

    private static final String CHAVE = "chave-de-teste-com-pelo-menos-32-caracteres";

    private TokenConfig tokenConfig;
    private Usuario usuario;

    @BeforeEach
    void preparar() {
        tokenConfig = new TokenConfig(CHAVE, 3600);

        usuario = Usuario.builder()
                .id(7L)
                .nome("Kauam")
                .email("kauam@troiafilmes.com")
                .senha("$2a$10$hash")
                .role(Role.ADMIN)
                .ativo(true)
                .build();
    }

    @Test
    @DisplayName("gera um token que carrega e-mail, id e papel do usuário")
    void geraTokenComOsDadosDoUsuario() {
        String token = tokenConfig.gerarToken(usuario);

        assertThat(tokenConfig.validarToken(token)).contains("kauam@troiafilmes.com");

        var decodificado = JWT.decode(token);
        assertThat(decodificado.getClaim("usuarioId").asLong()).isEqualTo(7L);
        assertThat(decodificado.getClaim("role").asString()).isEqualTo("ADMIN");
        assertThat(decodificado.getIssuer()).isEqualTo("troiafilmes-api");
    }

    @Test
    @DisplayName("rejeita token assinado com outra chave")
    void rejeitaTokenDeOutraChave() {
        String tokenIntruso = new TokenConfig("outra-chave-que-o-atacante-inventou-agora", 3600)
                .gerarToken(usuario);

        assertThat(tokenConfig.validarToken(tokenIntruso)).isEmpty();
    }

    @Test
    @DisplayName("rejeita token expirado")
    void rejeitaTokenExpirado() {
        // Validade negativa: o token já nasce vencido.
        String vencido = new TokenConfig(CHAVE, -60).gerarToken(usuario);

        assertThat(tokenConfig.validarToken(vencido)).isEmpty();
    }

    @Test
    @DisplayName("rejeita token de outro emissor, mesmo com a chave certa")
    void rejeitaTokenDeOutroEmissor() {
        String deOutraApi = JWT.create()
                .withIssuer("outra-api")
                .withSubject("kauam@troiafilmes.com")
                .withExpiresAt(Instant.now().plusSeconds(3600))
                .sign(Algorithm.HMAC256(CHAVE));

        assertThat(tokenConfig.validarToken(deOutraApi)).isEmpty();
    }

    @Test
    @DisplayName("rejeita token adulterado")
    void rejeitaTokenAdulterado() {
        String token = tokenConfig.gerarToken(usuario);
        String adulterado = token.substring(0, token.length() - 4) + "aaaa";

        assertThat(tokenConfig.validarToken(adulterado)).isEmpty();
    }

    @Test
    @DisplayName("rejeita texto que nem é um JWT")
    void rejeitaTextoQualquer() {
        assertThat(tokenConfig.validarToken("nao-e-um-token")).isEmpty();
    }

    @Test
    @DisplayName("informa a validade do token em segundos")
    void informaValidadeEmSegundos() {
        assertThat(tokenConfig.getExpiracaoSegundos()).isEqualTo(3600L);
    }
}
