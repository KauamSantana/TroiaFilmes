package br.edu.unesc.troiafilmes.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Documentação OpenAPI.
 *
 * <p>O esquema {@code bearerAuth} declarado aqui é o que faz aparecer o
 * botão "Authorize" no Swagger UI, permitindo testar os endpoints
 * protegidos direto do navegador.</p>
 */
@Configuration
public class OpenApiConfig {

    private static final String ESQUEMA_SEGURANCA = "bearerAuth";

    @Bean
    public OpenAPI troiaFilmesOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("TroiaFilmes API")
                        .version("1.0.0")
                        .description("""
                                API REST de um serviço de streaming de filmes, desenvolvida para o
                                Trabalho 1 da disciplina de Desenvolvimento Backend com Spring (UNESC).

                                ## Como autenticar
                                1. Chame `POST /api/auth/login` com um dos usuários abaixo.
                                2. Copie o `token` da resposta.
                                3. Clique em **Authorize** e cole o token.

                                | Papel | E-mail | Senha |
                                |-------|--------|-------|
                                | ADMIN | admin@troiafilmes.com | Admin@123 |
                                | USER  | usuario@troiafilmes.com | User@123 |
                                """)
                        .contact(new Contact()
                                .name("Equipe TroiaFilmes")
                                .email("contato@troiafilmes.com"))
                        .license(new License().name("Uso acadêmico")))

                .addSecurityItem(new SecurityRequirement().addList(ESQUEMA_SEGURANCA))
                .components(new Components().addSecuritySchemes(ESQUEMA_SEGURANCA,
                        new SecurityScheme()
                                .name(ESQUEMA_SEGURANCA)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Cole apenas o token, sem o prefixo Bearer.")));
    }
}
