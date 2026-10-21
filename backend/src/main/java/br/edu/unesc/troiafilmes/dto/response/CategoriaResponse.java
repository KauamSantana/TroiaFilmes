package br.edu.unesc.troiafilmes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Categoria do catálogo")
public record CategoriaResponse(

        Long id,
        String nome,
        String descricao
) {
}
