package br.edu.unesc.troiafilmes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados de uma categoria do catálogo")
public record CategoriaRequest(

        @NotBlank(message = "o nome da categoria é obrigatório")
        @Size(min = 2, max = 80, message = "o nome deve ter entre 2 e 80 caracteres")
        @Schema(example = "Ficção Científica")
        String nome,

        @Size(max = 255, message = "a descrição deve ter no máximo 255 caracteres")
        @Schema(example = "Futuros possíveis, tecnologia e exploração espacial.")
        String descricao
) {
}
