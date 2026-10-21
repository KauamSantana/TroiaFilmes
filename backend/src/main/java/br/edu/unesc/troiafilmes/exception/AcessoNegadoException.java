package br.edu.unesc.troiafilmes.exception;

/**
 * Lançada quando o usuário está autenticado, mas tenta acessar um recurso
 * que pertence a outra conta.
 *
 * <p>É a checagem de posse (ownership) que complementa a autorização por
 * papel: ser USER autenticado não basta, o perfil precisa ser seu.
 * Traduzida para HTTP 403 Forbidden.</p>
 */
public class AcessoNegadoException extends RuntimeException {

    public AcessoNegadoException(String mensagem) {
        super(mensagem);
    }

    public static AcessoNegadoException doRecurso(String recurso) {
        return new AcessoNegadoException(
                "%s não pertence ao usuário autenticado.".formatted(recurso));
    }
}
