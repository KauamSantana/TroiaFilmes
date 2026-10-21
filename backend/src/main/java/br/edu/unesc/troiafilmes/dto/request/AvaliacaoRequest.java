package br.edu.unesc.troiafilmes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Avaliação de um filme feita por um perfil.
 *
 * <p>O perfil vem do path da requisição e o dono é verificado no service,
 * por isso não faz parte do corpo.</p>
 */
@Schema(description = "Nota e comentário sobre um filme")
public record AvaliacaoRequest(

        @NotNull(message = "o id do filme é obrigatório")
        @Schema(example = "1")
        Long filmeId,

        @NotNull(message = "a nota é obrigatória")
        @Min(value = 1, message = "a nota mínima é 1")
        @Max(value = 5, message = "a nota máxima é 5")
        @Schema(example = "5")
        Integer nota,

        @Size(max = 2000, message = "o comentário deve ter no máximo 2000 caracteres")
        @Schema(example = "Melhor ficção científica que assisti este ano.")
        String comentario
) {
}
