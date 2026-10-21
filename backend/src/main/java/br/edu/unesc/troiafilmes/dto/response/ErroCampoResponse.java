package br.edu.unesc.troiafilmes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Erro de validação associado a um campo específico do corpo da requisição. */
@Schema(description = "Erro de validação de um campo")
public record ErroCampoResponse(

        @Schema(description = "Nome do campo com problema", example = "email")
        String campo,

        @Schema(description = "Descrição do problema", example = "deve ser um endereço de e-mail bem formado")
        String mensagem
) {
}
