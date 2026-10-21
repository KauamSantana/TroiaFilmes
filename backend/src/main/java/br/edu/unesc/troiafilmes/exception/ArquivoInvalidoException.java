package br.edu.unesc.troiafilmes.exception;

/**
 * Lançada quando um arquivo enviado não serve: vazio, formato não aceito,
 * extensão que não bate com o conteúdo real ou falha ao gravar em disco.
 *
 * <p>Traduzida para HTTP 400 Bad Request. Note que ela é diferente de
 * {@link RegraDeNegocioException}: aqui o problema está no que foi enviado,
 * não no estado do sistema.</p>
 */
public class ArquivoInvalidoException extends RuntimeException {

    public ArquivoInvalidoException(String mensagem) {
        super(mensagem);
    }

    public ArquivoInvalidoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
