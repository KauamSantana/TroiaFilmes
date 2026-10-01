package br.edu.unesc.troiafilmes.dto.response;

/** Devolvido no cadastro. Nunca inclui a senha, nem mesmo o hash. */
public record RegistroUsuarioResponse(String nome, String email) {
}
