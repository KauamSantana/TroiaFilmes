package br.edu.unesc.troiafilmes.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Publica as imagens enviadas pela administração.
 *
 * <p>São duas origens de imagem no sistema, e a diferença é proposital:</p>
 *
 * <ul>
 *   <li><b>{@code /posters/*}</b> — os pôsteres da carga inicial. Ficam em
 *       {@code src/main/resources/static}, então entram no JAR e são servidos
 *       pelo próprio Spring, sem configuração. Versionados junto com o código:
 *       quem clonar o projeto vê o catálogo ilustrado sem depender de rede.</li>
 *   <li><b>{@code /uploads/*}</b> — o que for enviado depois que a aplicação
 *       está no ar. Mora numa pasta externa, mapeada abaixo. Conteúdo de tempo
 *       de execução não pode entrar no artefato de build, senão cada deploy
 *       apagaria o que os usuários subiram.</li>
 * </ul>
 */
@Configuration
@EnableConfigurationProperties(ArmazenamentoProperties.class)
@RequiredArgsConstructor
public class RecursosEstaticosConfig implements WebMvcConfigurer {

    private final ArmazenamentoProperties propriedades;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registro) {
        Path raiz = Paths.get(propriedades.getDiretorio()).toAbsolutePath().normalize();

        registro.addResourceHandler(propriedades.getUrlBase() + "/**")
                .addResourceLocations(raiz.toUri().toString())
                .setCachePeriod(3600);
    }
}
