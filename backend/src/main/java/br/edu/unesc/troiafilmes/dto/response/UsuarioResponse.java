package br.edu.unesc.troiafilmes.dto.response;

import br.edu.unesc.troiafilmes.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/** Dados públicos de um usuário. A senha nunca é exposta. */
@Schema(description = "Usuário do TroiaFilmes")
public record UsuarioResponse(

        Long id,
        String nome,
        String email,
        Role role,
        Boolean ativo,
        LocalDateTime criadoEm
) {
}
