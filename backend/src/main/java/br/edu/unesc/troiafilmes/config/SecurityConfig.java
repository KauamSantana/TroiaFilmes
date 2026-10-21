package br.edu.unesc.troiafilmes.config;

import jakarta.servlet.DispatcherType;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Regras de acesso da API.
 *
 * <p>Com o Spring Security no projeto, toda rota nasce bloqueada. Aqui
 * liberamos só o necessário e declaramos quem pode o quê. A autorização
 * acontece em duas camadas:</p>
 *
 * <ol>
 *   <li>por rota e papel, declarada aqui e nos {@code @PreAuthorize} dos
 *       controllers;</li>
 *   <li>por posse do recurso, verificada nos services: um USER só mexe nos
 *       próprios perfis, avaliações, lista e histórico.</li>
 * </ol>
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(CorsProperties.class)
@RequiredArgsConstructor
public class SecurityConfig {

    private final SecurityFilter securityFilter;
    private final RespostaJsonDeSeguranca respostaJsonDeSeguranca;
    private final CorsProperties corsProperties;

    /** Rotas que acontecem antes de existir um token, e a documentação. */
    private static final String[] ROTAS_PUBLICAS = {
            "/api/auth/login",
            "/api/auth/registrar",
            "/api/auth/refresh",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF explora o cookie de sessão que o navegador envia sozinho.
                // Aqui não há sessão nem cookie: o token vai no header, à mão.
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // Nenhuma sessão no servidor: cada requisição traz o seu token.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 401 (sem token ou token inválido) e 403 (sem permissão) em JSON,
                // no mesmo formato do GlobalExceptionHandler.
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(respostaJsonDeSeguranca)
                        .accessDeniedHandler(respostaJsonDeSeguranca))
                .authorizeHttpRequests(auth -> auth
                        // A página de erro do Spring também passa por estas regras.
                        .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                        .requestMatchers(ROTAS_PUBLICAS).permitAll()
                        // O preflight do CORS chega sem token, antes da requisição real.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Imagens dos filmes: leitura pública, e não por descuido.
                        // A tag <img> do navegador não envia o header Authorization,
                        // então um pôster protegido por JWT simplesmente não
                        // carregaria. O envio continua restrito a ADMIN.
                        .requestMatchers(HttpMethod.GET, "/posters/**", "/uploads/**").permitAll()

                        // Catálogo: qualquer autenticado lê; só ADMIN escreve.
                        .requestMatchers(HttpMethod.GET, "/api/filmes/**", "/api/categorias/**", "/api/planos/**")
                        .authenticated()
                        .requestMatchers("/api/filmes/**", "/api/categorias/**", "/api/planos/**")
                        .hasRole("ADMIN")

                        // Administração de contas é exclusiva de ADMIN.
                        .requestMatchers("/api/usuarios/**").hasRole("ADMIN")

                        .anyRequest().authenticated())
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

    /** Libera o frontend React, que roda em outra origem (porta 5173). */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuracao = new CorsConfiguration();
        configuracao.setAllowedOrigins(corsProperties.getAllowedOrigins());
        configuracao.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuracao.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        configuracao.setExposedHeaders(List.of("Authorization"));
        configuracao.setAllowCredentials(true);
        configuracao.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource fonte = new UrlBasedCorsConfigurationSource();
        fonte.registerCorsConfiguration("/**", configuracao);
        return fonte;
    }
}
