package br.edu.unesc.troiafilmes.controller;

import br.edu.unesc.troiafilmes.config.TokenConfig;
import br.edu.unesc.troiafilmes.dto.request.LoginRequest;
import br.edu.unesc.troiafilmes.dto.request.RegistroUsuarioRequest;
import br.edu.unesc.troiafilmes.dto.response.LoginResponse;
import br.edu.unesc.troiafilmes.dto.response.RegistroUsuarioResponse;
import br.edu.unesc.troiafilmes.dto.response.UsuarioLogadoResponse;
import br.edu.unesc.troiafilmes.entity.Role;
import br.edu.unesc.troiafilmes.entity.Usuario;
import br.edu.unesc.troiafilmes.repository.UsuarioRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Cadastro, login e consulta de quem está autenticado. */
@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final TokenConfig tokenConfig;

    /**
     * Confere e-mail e senha e devolve o token.
     *
     * <p>A mensagem de erro é a mesma para e-mail inexistente e para senha
     * errada, de propósito: assim a API não revela quais e-mails têm conta.</p>
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        String email = normalizar(request.email());
        UsernamePasswordAuthenticationToken emailESenha =
                new UsernamePasswordAuthenticationToken(email, request.senha());

        Authentication autenticacao;
        try {
            autenticacao = authenticationManager.authenticate(emailESenha);
        } catch (AuthenticationException ex) {
            log.warn("Falha de login para {}", email);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "E-mail ou senha inválidos.");
        }

        Usuario usuario = (Usuario) autenticacao.getPrincipal();
        log.info("Login realizado: id={} role={}", usuario.getId(), usuario.getRole());

        return ResponseEntity.ok(new LoginResponse(tokenConfig.gerarToken(usuario)));
    }

    /**
     * Cria uma conta de assinante. O papel é sempre USER: ninguém vira ADMIN
     * por conta própria.
     */
    @PostMapping("/registrar")
    public ResponseEntity<RegistroUsuarioResponse> registrar(@Valid @RequestBody RegistroUsuarioRequest request) {
        String email = normalizar(request.email());

        if (usuarioRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Já existe uma conta com este e-mail.");
        }

        Usuario novoUsuario = Usuario.builder()
                .nome(request.nome().trim())
                .email(email)
                .senha(passwordEncoder.encode(request.senha()))
                .role(Role.USER)
                .build();
        usuarioRepository.save(novoUsuario);

        log.info("Nova conta criada: id={} email={}", novoUsuario.getId(), novoUsuario.getEmail());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new RegistroUsuarioResponse(novoUsuario.getNome(), novoUsuario.getEmail()));
    }

    /** Rota protegida: só responde com um token válido no header Authorization. */
    @GetMapping("/usuario-logado")
    public UsuarioLogadoResponse usuarioLogado(@AuthenticationPrincipal Usuario usuario) {
        return new UsuarioLogadoResponse(
                usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getRole());
    }

    /** "Fulano@Email.com" e "fulano@email.com" são a mesma conta. */
    private String normalizar(String email) {
        return email.trim().toLowerCase();
    }
}
