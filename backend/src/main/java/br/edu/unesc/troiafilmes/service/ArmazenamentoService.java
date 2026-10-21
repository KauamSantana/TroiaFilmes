package br.edu.unesc.troiafilmes.service;

import br.edu.unesc.troiafilmes.config.ArmazenamentoProperties;
import br.edu.unesc.troiafilmes.exception.ArquivoInvalidoException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.UUID;

/**
 * Gravação e remoção das imagens enviadas pela área administrativa.
 *
 * <p>Upload é a porta de entrada mais explorada de uma aplicação web, então a
 * validação aqui é deliberadamente desconfiada:</p>
 *
 * <ul>
 *   <li><b>O nome do arquivo do cliente é descartado.</b> Gravamos com um UUID
 *       novo. Um nome como {@code ../../application.properties} não teria como escapar
 *       da pasta de destino, porque o nome enviado nunca chega ao disco.</li>
 *   <li><b>O Content-Type declarado não é aceito como prova.</b> Ele é escolhido
 *       por quem envia. Conferimos os bytes iniciais do arquivo (a "assinatura"
 *       do formato) e é essa detecção que decide a extensão gravada.</li>
 *   <li><b>O tamanho é limitado em duas camadas</b>: no container, por
 *       {@code spring.servlet.multipart}, e aqui, para o caso do limite do
 *       container ser afrouxado sem que ninguém se lembre deste ponto.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArmazenamentoService {

    private final ArmazenamentoProperties propriedades;

    private Path raiz;

    @PostConstruct
    void prepararDiretorio() {
        raiz = Paths.get(propriedades.getDiretorio()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(raiz);
            log.info("Diretório de uploads: {}", raiz);
        } catch (IOException e) {
            throw new IllegalStateException("Não foi possível criar a pasta de uploads: " + raiz, e);
        }
    }

    /**
     * Grava a imagem e devolve a URL pública dela.
     *
     * @return caminho relativo servido pela aplicação, por exemplo
     *         {@code /uploads/2f1c....jpg}
     */
    public String salvarImagem(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("Envie um arquivo de imagem.");
        }

        if (arquivo.getSize() > propriedades.getTamanhoMaximoBytes()) {
            throw new ArquivoInvalidoException(
                    "A imagem tem %.1f MB e o limite é de %.1f MB.".formatted(
                            arquivo.getSize() / 1048576.0,
                            propriedades.getTamanhoMaximoBytes() / 1048576.0));
        }

        Formato formato = detectarFormato(arquivo);

        if (!propriedades.getTiposPermitidos().contains(formato.tipoMime())) {
            throw new ArquivoInvalidoException(
                    "Formato %s não aceito. Envie uma imagem %s.".formatted(
                            formato.tipoMime(),
                            String.join(", ", propriedades.getTiposPermitidos())));
        }

        String nome = UUID.randomUUID() + formato.extensao();
        Path destino = raiz.resolve(nome);

        try (InputStream entrada = arquivo.getInputStream()) {
            Files.copy(entrada, destino, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Falha ao gravar a imagem enviada.", e);
        }

        log.info("Imagem gravada: {} ({} bytes, {})", nome, arquivo.getSize(), formato.tipoMime());
        return propriedades.getUrlBase() + "/" + nome;
    }

    /**
     * Apaga um arquivo previamente gravado por este serviço.
     *
     * <p>Silenciosa de propósito: remover pôster de um filme não pode falhar
     * porque o arquivo já sumiu do disco. URLs que não sejam nossas são
     * ignoradas — é o que impede que um {@code posterUrl} apontando para
     * {@code ../application.properties} vire uma exclusão de arquivo do servidor.</p>
     */
    public void removerSeForNosso(String url) {
        if (url == null || !url.startsWith(propriedades.getUrlBase() + "/")) {
            return;
        }

        String nome = url.substring(propriedades.getUrlBase().length() + 1);
        Path alvo = raiz.resolve(nome).normalize();

        // Cinto e suspensório: mesmo com o nome vindo de uma URL nossa,
        // confirmamos que o caminho resolvido continua dentro da pasta.
        if (!alvo.startsWith(raiz)) {
            log.warn("Ignorando remoção fora da pasta de uploads: {}", url);
            return;
        }

        try {
            if (Files.deleteIfExists(alvo)) {
                log.info("Imagem removida: {}", nome);
            }
        } catch (IOException e) {
            log.warn("Não foi possível remover {}: {}", nome, e.getMessage());
        }
    }

    // ----------------------------------------------------------------- privados

    /** Formato reconhecido pelos bytes iniciais do arquivo. */
    private record Formato(String tipoMime, String extensao) {}

    private static final byte[] ASSINATURA_PNG =
            {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    /**
     * Identifica o formato pelos primeiros bytes, ignorando o que o cliente
     * declarou. É o que diferencia uma imagem de um executável renomeado.
     */
    private Formato detectarFormato(MultipartFile arquivo) {
        byte[] inicio = new byte[12];
        int lidos;

        try (InputStream entrada = arquivo.getInputStream()) {
            lidos = entrada.readNBytes(inicio, 0, inicio.length);
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Não foi possível ler o arquivo enviado.", e);
        }

        if (lidos < 12) {
            throw new ArquivoInvalidoException("O arquivo enviado não é uma imagem válida.");
        }

        // JPEG: FF D8 FF
        if ((inicio[0] & 0xFF) == 0xFF && (inicio[1] & 0xFF) == 0xD8 && (inicio[2] & 0xFF) == 0xFF) {
            return new Formato("image/jpeg", ".jpg");
        }

        if (Arrays.equals(Arrays.copyOf(inicio, 8), ASSINATURA_PNG)) {
            return new Formato("image/png", ".png");
        }

        // WebP: "RIFF" nos bytes 0-3 e "WEBP" nos bytes 8-11.
        if (combina(inicio, 0, "RIFF") && combina(inicio, 8, "WEBP")) {
            return new Formato("image/webp", ".webp");
        }

        throw new ArquivoInvalidoException(
                "O arquivo enviado não é uma imagem JPEG, PNG ou WebP.");
    }

    private static boolean combina(byte[] dados, int posicao, String texto) {
        for (int i = 0; i < texto.length(); i++) {
            if (dados[posicao + i] != texto.charAt(i)) {
                return false;
            }
        }
        return true;
    }
}
