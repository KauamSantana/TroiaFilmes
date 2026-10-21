package br.edu.unesc.troiafilmes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Item da Minha Lista de um perfil")
public record ItemListaResponse(

        Long id,
        Long perfilId,
        FilmeResponse filme,
        LocalDateTime adicionadoEm
) {
}
