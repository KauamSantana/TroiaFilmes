package br.edu.unesc.troiafilmes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

/**
 * Envelope de paginação da API.
 *
 * <p>Existe para não serializar o {@code PageImpl} do Spring direto na
 * resposta: aquele formato não tem contrato estável entre versões e expõe
 * detalhes internos do framework. Aqui o contrato é nosso.</p>
 *
 * @param <T> tipo do DTO de saída
 */
@Schema(description = "Resultado paginado")
public record PaginaResponse<T>(

        @Schema(description = "Itens da página atual")
        List<T> conteudo,

        @Schema(description = "Número da página atual, começando em 0", example = "0")
        int pagina,

        @Schema(description = "Quantidade de itens por página", example = "12")
        int tamanho,

        @Schema(description = "Total de itens em todas as páginas", example = "24")
        long totalElementos,

        @Schema(description = "Total de páginas", example = "2")
        int totalPaginas,

        @Schema(description = "Indica se esta é a última página", example = "false")
        boolean ultima
) {

    /** Converte uma {@link Page} de entidades em uma página de DTOs. */
    public static <E, D> PaginaResponse<D> de(Page<E> page, Function<E, D> conversor) {
        return new PaginaResponse<>(
                page.getContent().stream().map(conversor).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }

    /** Variante para quando os DTOs já foram montados fora daqui. */
    public static <E, D> PaginaResponse<D> de(Page<E> page, List<D> conteudo) {
        return new PaginaResponse<>(
                conteudo,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}
