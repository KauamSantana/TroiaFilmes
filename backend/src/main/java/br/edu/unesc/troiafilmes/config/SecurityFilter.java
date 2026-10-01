package br.edu.unesc.troiafilmes.config;

import br.edu.unesc.troiafilmes.entity.Usuario;
import br.edu.unesc.troiafilmes.repository.UsuarioRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Roda uma vez em cada requisição: lê o header
 * {@code Authorization: Bearer <token>}, valida o token e, se estiver tudo
 * certo, coloca o usuário no contexto de segurança.
 *
 * <p>Sem token, ou com token inválido, o filtro só segue adiante sem
 * autenticar. Quem decide se a rota exigia login é o {@link SecurityConfig}.</p>
 */
@Component
@RequiredArgsConstructor
public class SecurityFilter extends OncePerRequestFilter {

    private static final String PREFIXO = "Bearer ";

    private final TokenConfig tokenConfig;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = recuperarToken(request);

        if (token != null) {
            tokenConfig.validarToken(token)
                    .flatMap(usuarioRepository::findByEmail)
                    // Conta desativada depois de o token ter sido emitido.
                    .filter(Usuario::isEnabled)
                    .ifPresent(usuario -> SecurityContextHolder.getContext().setAuthentication(
                            new UsernamePasswordAuthenticationToken(
                                    usuario, null, usuario.getAuthorities())));
        }

        filterChain.doFilter(request, response);
    }

    private String recuperarToken(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith(PREFIXO)) {
            return null;
        }
        String token = header.substring(PREFIXO.length()).trim();
        return token.isEmpty() ? null : token;
    }
}
