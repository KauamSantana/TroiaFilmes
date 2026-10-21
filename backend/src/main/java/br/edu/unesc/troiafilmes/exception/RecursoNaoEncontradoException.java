package br.edu.unesc.troiafilmes.exception;

/**
 * Lançada quando um recurso referenciado pela requisição não existe.
 * Traduzida para HTTP 404 pelo {@link GlobalExceptionHandler}.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }

    /** Atalho para a mensagem padrão: "Filme de id 42 não encontrado." */
    public static RecursoNaoEncontradoException de(String recurso, Object id) {
        return new RecursoNaoEncontradoException(
                "%s de id %s não encontrado.".formatted(recurso, id));
    }
}
