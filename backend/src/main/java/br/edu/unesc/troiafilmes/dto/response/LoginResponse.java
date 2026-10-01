package br.edu.unesc.troiafilmes.dto.response;

/** Devolvido no login: apenas o token, que vai no header {@code Authorization: Bearer <token>}. */
public record LoginResponse(String token) {
}
