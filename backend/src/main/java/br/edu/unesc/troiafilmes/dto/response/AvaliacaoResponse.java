package br.edu.unesc.troiafilmes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Avaliação de um filme por um perfil")
public record AvaliacaoResponse(

        Long id,
        Long perfilId,
        String perfilNome,
        Long filmeId,
        String filmeTitulo,
        Integer nota,
        String comentario,
        LocalDateTime criadoEm
) {
}
