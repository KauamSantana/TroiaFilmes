package br.edu.unesc.troiafilmes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Devolvido no cadastro. Nunca inclui a senha, nem mesmo o hash. */
@Schema(description = "Conta recém-criada")
public record RegistroUsuarioResponse(

        @Schema(example = "Estefani Souza")
        String nome,

        @Schema(example = "estefani@exemplo.com")
        String email
) {
}
