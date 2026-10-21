package br.edu.unesc.troiafilmes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.util.Set;

/** Dados de cadastro e atualização de um filme. Exclusivo de ADMIN. */
@Schema(description = "Dados de um filme do catálogo")
public record FilmeRequest(

        @NotBlank(message = "o título é obrigatório")
        @Size(min = 1, max = 180, message = "o título deve ter no máximo 180 caracteres")
        @Schema(example = "Interestelar")
        String titulo,

        @Size(max = 4000, message = "a sinopse deve ter no máximo 4000 caracteres")
        @Schema(example = "Com a Terra se tornando inabitável, um ex-piloto atravessa um buraco de minhoca.")
        String sinopse,

        @NotNull(message = "o ano de lançamento é obrigatório")
        @Min(value = 1888, message = "o ano de lançamento não pode ser anterior a 1888")
        @Max(value = 2200, message = "o ano de lançamento informado é inválido")
        @Schema(example = "2014")
        Integer anoLancamento,

        @NotNull(message = "a duração é obrigatória")
        @Positive(message = "a duração deve ser maior que zero")
        @Max(value = 1000, message = "a duração informada é inválida")
        @Schema(example = "169")
        Integer duracaoMinutos,

        @Size(max = 500, message = "a URL do pôster deve ter no máximo 500 caracteres")
        @Schema(description = "Endereço da imagem. Para enviar um arquivo em vez de "
                + "informar um endereço, use POST /api/filmes/{id}/poster.",
                example = "/posters/interestelar.jpg")
        String posterUrl,

        @NotEmpty(message = "informe ao menos uma categoria")
        @Schema(description = "Ids das categorias do filme", example = "[4, 6]")
        Set<Long> categoriaIds
) {
}
