package br.edu.unesc.troiafilmes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Posição de reprodução registrada pelo player. */
@Schema(description = "Progresso de visualização de um filme")
public record ProgressoRequest(

        @NotNull(message = "o id do filme é obrigatório")
        @Schema(example = "1")
        Long filmeId,

        @NotNull(message = "informe os minutos assistidos")
        @Min(value = 0, message = "os minutos assistidos não podem ser negativos")
        @Schema(example = "45")
        Integer minutosAssistidos
) {
}
