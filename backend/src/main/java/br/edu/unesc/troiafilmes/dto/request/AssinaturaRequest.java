package br.edu.unesc.troiafilmes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Plano a ser contratado")
public record AssinaturaRequest(

        @NotNull(message = "o id do plano é obrigatório")
        @Schema(example = "3")
        Long planoId
) {
}
