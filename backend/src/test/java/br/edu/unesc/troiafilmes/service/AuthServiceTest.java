package br.edu.unesc.troiafilmes.service;

import br.edu.unesc.troiafilmes.config.TokenConfig;
import br.edu.unesc.troiafilmes.dto.request.LoginRequest;
import br.edu.unesc.troiafilmes.dto.request.RegistroUsuarioRequest;
import br.edu.unesc.troiafilmes.dto.response.LoginResponse;
import br.edu.unesc.troiafilmes.entity.*;
import br.edu.unesc.troiafilmes.exception.RegraDeNegocioException;
import br.edu.unesc.troiafilmes.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AuthService")
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PerfilRepository perfilRepository;
    @Mock private PlanoRepository planoRepository;
    @Mock private AssinaturaRepository assinaturaRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private TokenConfig tokenConfig;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void preparar() {
        ReflectionTestUtils.setField(authService, "refreshExpiracaoSegundos", 604800L);
        when(passwordEncoder.encode(any())).thenReturn("$2a$10$hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });
        when(tokenConfig.gerarToken(any())).thenReturn("token-de-acesso");
        when(tokenConfig.getExpiracaoSegundos()).thenReturn(3600L);
    }

    @Test
    @DisplayName("recusa cadastro com e-mail já usado, sem diferenciar maiúsculas")
    void recusaEmailRepetido() {
        when(usuarioRepository.existsByEmail("estefani@exemplo.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registrar(
                new RegistroUsuarioRequest("Estefani", "Estefani@Exemplo.com", "Senha@123")))
                .isInstanceOf(RegraDeNegocioException.class);

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("cria a conta como USER, no plano Básico e com o perfil Principal")
    void cadastroCriaContaPlanoEPerfil() {
        Plano basico = Plano.builder().id(1L).nome("Básico").preco(new BigDecimal("19.90"))
                .maxPerfis(1).qualidadeMaxima(Qualidade.SD).build();
        when(planoRepository.findByNomeIgnoreCase("Básico")).thenReturn(Optional.of(basico));

        var resposta = authService.registrar(
                new RegistroUsuarioRequest("  Estefani  ", "Estefani@Exemplo.com", "Senha@123"));

        ArgumentCaptor<Usuario> usuario = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(usuario.capture());
        assertThat(usuario.getValue().getRole()).isEqualTo(Role.USER);
        assertThat(usuario.getValue().getEmail()).isEqualTo("estefani@exemplo.com");
        assertThat(usuario.getValue().getSenha()).isEqualTo("$2a$10$hash");

        ArgumentCaptor<Assinatura> assinatura = ArgumentCaptor.forClass(Assinatura.class);
        verify(assinaturaRepository).save(assinatura.capture());
        assertThat(assinatura.getValue().getPlano()).isEqualTo(basico);
        assertThat(assinatura.getValue().getStatus()).isEqualTo(StatusAssinatura.ATIVA);

        ArgumentCaptor<Perfil> perfil = ArgumentCaptor.forClass(Perfil.class);
        verify(perfilRepository).save(perfil.capture());
        assertThat(perfil.getValue().getNome()).isEqualTo("Principal");

        assertThat(resposta.nome()).isEqualTo("Estefani");
        assertThat(resposta.email()).isEqualTo("estefani@exemplo.com");
    }

    @Test
    @DisplayName("login devolve token, refresh token e os dados do usuário")
    void loginEmiteTokens() {
        Usuario usuario = usuario(true);
        when(authenticationManager.authenticate(any()))
                .thenReturn(new UsernamePasswordAuthenticationToken(usuario, null, usuario.getAuthorities()));

        LoginResponse resposta = authService.login(new LoginRequest("USUARIO@troiafilmes.com", "User@123"));

        assertThat(resposta.token()).isEqualTo("token-de-acesso");
        assertThat(resposta.refreshToken()).isNotBlank();
        assertThat(resposta.tipo()).isEqualTo("Bearer");
        assertThat(resposta.usuario().email()).isEqualTo("usuario@troiafilmes.com");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("login recusado vira a mesma mensagem genérica, seja qual for o motivo")
    void loginRecusadoTemMensagemGenerica() {
        when(authenticationManager.authenticate(any())).thenThrow(new DisabledException("Conta desativada"));

        assertThatThrownBy(() -> authService.login(new LoginRequest("usuario@troiafilmes.com", "User@123")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("E-mail ou senha inválidos.");
    }

    @Test
    @DisplayName("recusa refresh token já revogado")
    void recusaRefreshRevogado() {
        RefreshToken revogado = RefreshToken.builder().token("abc").usuario(usuario(true))
                .expiraEm(LocalDateTime.now().plusDays(1)).revogado(true).build();
        when(refreshTokenRepository.buscarComUsuario("abc")).thenReturn(Optional.of(revogado));

        assertThatThrownBy(() -> authService.renovar("abc")).isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("troca o refresh token por um novo par e revoga o antigo")
    void renovarRevogaOAntigo() {
        RefreshToken valido = RefreshToken.builder().token("abc").usuario(usuario(true))
                .expiraEm(LocalDateTime.now().plusDays(1)).revogado(false).build();
        when(refreshTokenRepository.buscarComUsuario("abc")).thenReturn(Optional.of(valido));

        LoginResponse resposta = authService.renovar("abc");

        assertThat(valido.getRevogado()).isTrue();
        assertThat(resposta.refreshToken()).isNotEqualTo("abc");
    }

    private static Usuario usuario(boolean ativo) {
        return Usuario.builder().id(2L).nome("Usuário Demonstração").email("usuario@troiafilmes.com")
                .senha("$2a$10$hash").role(Role.USER).ativo(ativo).build();
    }
}
