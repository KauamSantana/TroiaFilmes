package br.edu.unesc.troiafilmes.config;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Regras de acesso da API.
 *
 * <p>Com o Spring Security no projeto, toda rota nasce bloqueada. Aqui
 * liberamos só o necessário: o login e o cadastro, que por definição
 * acontecem antes de existir um token. Todo o resto exige token.</p>
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityFilter securityFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF explora o cookie de sessão que o navegador envia sozinho.
                // Aqui não há sessão nem cookie: o token vai no header, à mão.
                .csrf(csrf -> csrf.disable())
                // O frontend React (Aula 12) terá a sua origem liberada aqui.
                .cors(Customizer.withDefaults())
                // Nenhuma sessão no servidor: cada requisição traz o seu token.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Sem isso, um erro 400 ou 404 viraria 401: a página de
                        // erro do Spring também passa por estas regras.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/registrar").permitAll()
                        .anyRequest().authenticated())
                // Sem token, ou com token inválido: 401. O padrão do Spring seria
                // 403, que significa outra coisa (logado, mas sem permissão).
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, erro) ->
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED,
                                "Token ausente, inválido ou expirado.")))
                // O filtro do token roda antes do filtro padrão de usuário e senha.
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /** Usado no login para conferir e-mail e senha através do {@link AuthConfig}. */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }
}
