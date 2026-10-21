package br.edu.unesc.troiafilmes.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Parâmetros do armazenamento de imagens enviadas, lidos do bloco
 * {@code troiafilmes.armazenamento} do application.properties.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "troiafilmes.armazenamento")
public class ArmazenamentoProperties {

    /**
     * Pasta onde os arquivos enviados são gravados. Fica fora do JAR de
     * propósito: conteúdo enviado em tempo de execução não pertence ao
     * artefato de build, que deve ser reproduzível.
     */
    private String diretorio = "uploads";

    /** Prefixo público pelo qual os arquivos gravados são servidos. */
    private String urlBase = "/uploads";

    /** Tamanho máximo aceito por imagem, em bytes. */
    private long tamanhoMaximoBytes = 2L * 1024 * 1024;

    /** Tipos de imagem aceitos. */
    private List<String> tiposPermitidos = List.of("image/jpeg", "image/png", "image/webp");
}
