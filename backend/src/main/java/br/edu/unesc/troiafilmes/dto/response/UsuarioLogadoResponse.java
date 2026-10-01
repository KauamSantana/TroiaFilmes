package br.edu.unesc.troiafilmes.dto.response;

import br.edu.unesc.troiafilmes.entity.Role;

/** Quem é o dono do token enviado na requisição. */
public record UsuarioLogadoResponse(Long id, String nome, String email, Role role) {
}
