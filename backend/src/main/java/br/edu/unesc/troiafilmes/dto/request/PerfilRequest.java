package br.edu.unesc.troiafilmes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados de um perfil de consumo")
public record PerfilRequest(

        @NotBlank(message = "o nome do perfil é obrigatório")
        @Size(min = 2, max = 60, message = "o nome deve ter entre 2 e 60 caracteres")
        @Schema(example = "Principal")
        String nome,

        @Size(max = 500, message = "a URL do avatar deve ter no máximo 500 caracteres")
        @Schema(example = "/avatars/azul.svg")
        String avatarUrl,

        @Schema(description = "Marca o perfil como infantil", example = "false")
        Boolean infantil
) {
}
