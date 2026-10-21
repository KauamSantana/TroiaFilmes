package br.edu.unesc.troiafilmes.service;

import br.edu.unesc.troiafilmes.config.TokenConfig;
import br.edu.unesc.troiafilmes.dto.request.LoginRequest;
import br.edu.unesc.troiafilmes.dto.request.RegistroUsuarioRequest;
import br.edu.unesc.troiafilmes.dto.response.LoginResponse;
import br.edu.unesc.troiafilmes.dto.response.RegistroUsuarioResponse;
import br.edu.unesc.troiafilmes.entity.*;
import br.edu.unesc.troiafilmes.exception.RegraDeNegocioException;
import br.edu.unesc.troiafilmes.mapper.UsuarioMapper;
import br.edu.unesc.troiafilmes.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Cadastro, login e ciclo de vida da sessão.
 *
 * <p>Regras de negócio aplicadas aqui:</p>
 * <ul>
 *   <li>e-mail é único no sistema, sem diferenciar maiúsculas;</li>
 *   <li>conta desativada não autentica, mesmo com a senha correta;</li>
 *   <li>ninguém vira ADMIN por cadastro próprio: o papel é sempre USER;</li>
 *   <li>todo novo assinante entra no plano Básico já com um perfil criado,
 *       para não ficar preso numa conta sem nada utilizável.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    /** Plano atribuído automaticamente a quem acabou de se cadastrar. */
    private static final String PLANO_DE_ENTRADA = "Básico";

    private static final String PERFIL_PADRAO = "Principal";

    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PlanoRepository planoRepository;
    private final AssinaturaRepository assinaturaRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenConfig tokenConfig;

    @Value("${troiafilmes.jwt.refresh-expiracao-segundos}")
    private long refreshExpiracaoSegundos;

    // --------------------------------------------------------------- cadastro

    @Transactional
    public RegistroUsuarioResponse registrar(RegistroUsuarioRequest request) {
        String email = normalizar(request.email());

        if (usuarioRepository.existsByEmail(email)) {
            throw new RegraDeNegocioException("Já existe uma conta com este e-mail.");
        }

        Usuario usuario = usuarioRepository.save(Usuario.builder()
                .nome(request.nome().trim())
                .email(email)
                .senha(passwordEncoder.encode(request.senha()))
                .role(Role.USER)
                .ativo(true)
                .build());

        criarAssinaturaDeEntrada(usuario);
        criarPerfilPadrao(usuario);

        log.info("Nova conta criada: id={} email={}", usuario.getId(), usuario.getEmail());

        return new RegistroUsuarioResponse(usuario.getNome(), usuario.getEmail());
    }

    private void criarAssinaturaDeEntrada(Usuario usuario) {
        planoRepository.findByNomeIgnoreCase(PLANO_DE_ENTRADA).ifPresent(plano ->
                assinaturaRepository.save(Assinatura.builder()
                        .usuario(usuario)
                        .plano(plano)
                        .status(StatusAssinatura.ATIVA)
                        .build()));
    }

    private void criarPerfilPadrao(Usuario usuario) {
        perfilRepository.save(Perfil.builder()
                .usuario(usuario)
                .nome(PERFIL_PADRAO)
                .infantil(false)
                .build());
    }

    // ------------------------------------------------------------------ login

    /**
     * Confere e-mail e senha pelo {@link AuthenticationManager}, que usa o
     * {@code AuthConfig} para buscar a conta e o BCrypt para comparar a senha.
     *
     * <p>A mensagem de erro é a mesma para e-mail inexistente, senha errada e
     * conta desativada, de propósito: assim a API não revela quais e-mails
     * têm conta.</p>
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = normalizar(request.email());

        Usuario usuario;
        try {
            usuario = (Usuario) authenticationManager
                    .authenticate(new UsernamePasswordAuthenticationToken(email, request.senha()))
                    .getPrincipal();
        } catch (AuthenticationException ex) {
            log.warn("Falha de login para {}", email);
            throw new BadCredentialsException("E-mail ou senha inválidos.");
        }

        log.info("Login realizado: id={} role={}", usuario.getId(), usuario.getRole());
        return emitirTokens(usuario);
    }

    // ----------------------------------------------------------- refresh/logout

    /**
     * Troca um refresh token válido por um novo par de tokens.
     *
     * <p>O token usado é revogado na troca (rotação): um refresh token
     * capturado não continua valendo depois de ser usado.</p>
     */
    @Transactional
    public LoginResponse renovar(String refreshToken) {
        RefreshToken armazenado = refreshTokenRepository.buscarComUsuario(refreshToken)
                .orElseThrow(() -> new BadCredentialsException("Refresh token inválido."));

        if (!armazenado.estaValido()) {
            throw new BadCredentialsException("Refresh token expirado ou revogado. Faça login novamente.");
        }

        Usuario usuario = armazenado.getUsuario();
        if (!usuario.isEnabled()) {
            throw new DisabledException("Conta desativada.");
        }

        armazenado.setRevogado(true);
        refreshTokenRepository.save(armazenado);

        return emitirTokens(usuario);
    }

    /**
     * Revoga todos os refresh tokens do usuário, encerrando as sessões abertas.
     * O token de acesso atual continua válido até expirar, por ser autocontido.
     */
    @Transactional
    public void logout(Long usuarioId) {
        int revogados = refreshTokenRepository.revogarTodosDoUsuario(usuarioId);
        log.info("Logout do usuário {}: {} refresh token(s) revogado(s)", usuarioId, revogados);
    }

    // ---------------------------------------------------------------- privados

    private LoginResponse emitirTokens(Usuario usuario) {
        return LoginResponse.de(
                tokenConfig.gerarToken(usuario),
                criarRefreshToken(usuario),
                tokenConfig.getExpiracaoSegundos(),
                UsuarioMapper.paraResponse(usuario));
    }

    /**
     * Gera um refresh token opaco e o persiste na tabela {@code refresh_token}.
     *
     * <p>É um valor aleatório, e não um JWT, justamente porque precisa poder
     * ser revogado: um JWT assinado vale até expirar, sem como cancelar.</p>
     */
    private String criarRefreshToken(Usuario usuario) {
        String token = UUID.randomUUID() + "-" + UUID.randomUUID();

        refreshTokenRepository.save(RefreshToken.builder()
                .usuario(usuario)
                .token(token)
                .expiraEm(LocalDateTime.now().plusSeconds(refreshExpiracaoSegundos))
                .revogado(false)
                .build());

        return token;
    }

    /** "Fulano@Email.com" e "fulano@email.com" são a mesma conta. */
    private String normalizar(String email) {
        return email.trim().toLowerCase();
    }
}
