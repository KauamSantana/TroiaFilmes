package br.edu.unesc.troiafilmes.exception;

/**
 * Lançada quando a requisição é sintaticamente válida, mas viola uma regra
 * de negócio do TroiaFilmes (avaliação duplicada, limite de perfis do plano,
 * categoria ainda em uso, e-mail já cadastrado...).
 *
 * <p>Traduzida para HTTP 409 Conflict.</p>
 */
public class RegraDeNegocioException extends RuntimeException {

    public RegraDeNegocioException(String mensagem) {
        super(mensagem);
    }
}
