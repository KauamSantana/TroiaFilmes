package br.edu.unesc.troiafilmes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Progresso de visualização de um filme")
public record HistoricoResponse(

        Long id,
        Long perfilId,
        FilmeResponse filme,

        @Schema(example = "45")
        Integer minutosAssistidos,

        @Schema(description = "Percentual assistido, de 0 a 100", example = "37")
        Integer percentualAssistido,

        @Schema(description = "Marcado quando o progresso atinge 90% da duração")
        Boolean concluido,

        LocalDateTime assistidoEm
) {
}
