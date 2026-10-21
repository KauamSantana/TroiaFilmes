package br.edu.unesc.troiafilmes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Filme com suas categorias e o resumo das avaliações recebidas.
 *
 * <p>{@code notaMedia} e {@code totalAvaliacoes} são calculados por consulta
 * de agregação, não ficam armazenados na tabela.</p>
 */
@Schema(description = "Filme do catálogo")
public record FilmeResponse(

        Long id,
        String titulo,
        String sinopse,
        Integer anoLancamento,
        Integer duracaoMinutos,
        String posterUrl,
        LocalDateTime criadoEm,

        @Schema(description = "Categorias às quais o filme pertence")
        List<CategoriaResponse> categorias,

        @Schema(description = "Média das notas recebidas, de 1 a 5", example = "4.5")
        Double notaMedia,

        @Schema(description = "Quantidade de avaliações recebidas", example = "12")
        Long totalAvaliacoes
) {
}
