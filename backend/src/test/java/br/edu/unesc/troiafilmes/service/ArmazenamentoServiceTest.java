package br.edu.unesc.troiafilmes.service;

import br.edu.unesc.troiafilmes.config.ArmazenamentoProperties;
import br.edu.unesc.troiafilmes.exception.ArquivoInvalidoException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Validação do upload de imagens.
 *
 * <p>O foco destes testes é o que um cliente mal-intencionado consegue enviar:
 * um arquivo que <em>diz</em> ser imagem, um nome que tenta escapar da pasta,
 * um arquivo grande demais. Por isso quase todos os casos são de recusa.</p>
 */
@DisplayName("ArmazenamentoService")
class ArmazenamentoServiceTest {

    @TempDir
    Path pasta;

    private ArmazenamentoService servico;
    private ArmazenamentoProperties propriedades;

    /** Cabeçalho mínimo de um JPEG: SOI seguido de um marcador. */
    private static final byte[] JPEG_INICIO = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0x00, 0x10, 'J', 'F', 'I', 'F', 0x00, 0x01
    };

    private static final byte[] PNG_INICIO = {
            (byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D
    };

    @BeforeEach
    void preparar() {
        propriedades = new ArmazenamentoProperties();
        propriedades.setDiretorio(pasta.toString());
        propriedades.setUrlBase("/uploads");
        propriedades.setTamanhoMaximoBytes(1024);

        servico = new ArmazenamentoService(propriedades);
        servico.prepararDiretorio();
    }

    private static MockMultipartFile arquivo(String nome, String tipo, byte[] conteudo) {
        return new MockMultipartFile("imagem", nome, tipo, conteudo);
    }

    private static byte[] comTamanho(byte[] inicio, int total) {
        byte[] dados = new byte[total];
        System.arraycopy(inicio, 0, dados, 0, Math.min(inicio.length, total));
        return dados;
    }

    @Nested
    @DisplayName("aceita")
    class Aceita {

        @Test
        @DisplayName("JPEG válido, gravando com nome novo em vez do nome enviado")
        void gravaJpeg() throws IOException {
            String url = servico.salvarImagem(
                    arquivo("ferias na praia.jpg", "image/jpeg", comTamanho(JPEG_INICIO, 500)));

            assertThat(url).startsWith("/uploads/").endsWith(".jpg");

            // O nome enviado não sobrevive: nada do original aparece no disco.
            String nomeGravado = url.substring("/uploads/".length());
            assertThat(nomeGravado).doesNotContain("ferias").doesNotContain(" ");
            assertThat(Files.exists(pasta.resolve(nomeGravado))).isTrue();
            assertThat(Files.size(pasta.resolve(nomeGravado))).isEqualTo(500);
        }

        @Test
        @DisplayName("PNG válido, com a extensão vinda do conteúdo e não do nome")
        void gravaPng() {
            // O nome diz .jpg, mas os bytes dizem PNG. Vale o conteúdo.
            String url = servico.salvarImagem(
                    arquivo("disfarcado.jpg", "image/jpeg", comTamanho(PNG_INICIO, 300)));

            assertThat(url).endsWith(".png");
        }
    }

    @Nested
    @DisplayName("recusa")
    class Recusa {

        @Test
        @DisplayName("arquivo vazio")
        void arquivoVazio() {
            assertThatThrownBy(() -> servico.salvarImagem(
                    arquivo("vazio.jpg", "image/jpeg", new byte[0])))
                    .isInstanceOf(ArquivoInvalidoException.class)
                    .hasMessageContaining("Envie um arquivo");
        }

        @Test
        @DisplayName("arquivo que se declara imagem mas tem conteúdo de texto")
        void conteudoNaoEhImagem() {
            byte[] script = "<?php system($_GET['c']); ?>".getBytes(StandardCharsets.UTF_8);

            assertThatThrownBy(() -> servico.salvarImagem(
                    arquivo("inofensivo.png", "image/png", script)))
                    .isInstanceOf(ArquivoInvalidoException.class)
                    .hasMessageContaining("não é uma imagem");

            assertThat(pasta.toFile().list()).isEmpty();
        }

        @Test
        @DisplayName("arquivo curto demais para ter assinatura de formato")
        void arquivoMinusculo() {
            assertThatThrownBy(() -> servico.salvarImagem(
                    arquivo("curto.jpg", "image/jpeg", new byte[]{(byte) 0xFF, (byte) 0xD8})))
                    .isInstanceOf(ArquivoInvalidoException.class);
        }

        @Test
        @DisplayName("imagem acima do tamanho máximo")
        void acimaDoLimite() {
            assertThatThrownBy(() -> servico.salvarImagem(
                    arquivo("grande.jpg", "image/jpeg", comTamanho(JPEG_INICIO, 2048))))
                    .isInstanceOf(ArquivoInvalidoException.class)
                    .hasMessageContaining("limite");

            assertThat(pasta.toFile().list()).isEmpty();
        }

        @Test
        @DisplayName("GIF, que não está na lista de formatos aceitos")
        void formatoForaDaLista() {
            byte[] gif = comTamanho("GIF89a".getBytes(StandardCharsets.US_ASCII), 100);

            assertThatThrownBy(() -> servico.salvarImagem(arquivo("animado.gif", "image/gif", gif)))
                    .isInstanceOf(ArquivoInvalidoException.class);
        }
    }

    @Nested
    @DisplayName("na remoção")
    class Remocao {

        @Test
        @DisplayName("apaga o arquivo que ela mesma gravou")
        void apagaProprio() {
            String url = servico.salvarImagem(
                    arquivo("capa.jpg", "image/jpeg", comTamanho(JPEG_INICIO, 200)));

            servico.removerSeForNosso(url);

            assertThat(pasta.toFile().list()).isEmpty();
        }

        @Test
        @DisplayName("ignora endereços externos, que não são arquivos nossos")
        void ignoraUrlExterna() {
            String url = servico.salvarImagem(
                    arquivo("capa.jpg", "image/jpeg", comTamanho(JPEG_INICIO, 200)));

            servico.removerSeForNosso("https://exemplo.com/imagem.jpg");
            servico.removerSeForNosso("/posters/matrix.jpg");
            servico.removerSeForNosso(null);

            // Nada foi apagado: o arquivo gravado continua lá.
            assertThat(url).isNotNull();
            assertThat(pasta.toFile().list()).hasSize(1);
        }

        @Test
        @DisplayName("não sai da pasta de uploads mesmo com caminho relativo na URL")
        void naoEscapaDaPasta() throws IOException {
            Path fora = pasta.getParent().resolve("nao-me-apague.txt");
            Files.writeString(fora, "arquivo de fora");

            servico.removerSeForNosso("/uploads/../" + fora.getFileName());

            assertThat(Files.exists(fora)).isTrue();
        }
    }
}
