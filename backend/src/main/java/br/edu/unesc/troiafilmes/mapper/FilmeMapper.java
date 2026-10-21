package br.edu.unesc.troiafilmes.mapper;

import br.edu.unesc.troiafilmes.dto.response.CategoriaResponse;
import br.edu.unesc.troiafilmes.dto.response.FilmeResponse;
import br.edu.unesc.troiafilmes.entity.Categoria;
import br.edu.unesc.troiafilmes.entity.Filme;

import java.util.Comparator;
import java.util.List;

public final class FilmeMapper {

    private FilmeMapper() {
    }

    /** Converte sem resumo de avaliações (nota média fica zerada). */
    public static FilmeResponse paraResponse(Filme filme) {
        return paraResponse(filme, null, 0L);
    }

    /**
     * Converte o filme incluindo o resumo das avaliações.
     *
     * @param notaMedia       média calculada por agregação, ou {@code null} se ainda não houver avaliações
     * @param totalAvaliacoes quantidade de avaliações recebidas
     */
    public static FilmeResponse paraResponse(Filme filme, Double notaMedia, Long totalAvaliacoes) {
        List<CategoriaResponse> categorias = filme.getCategorias().stream()
                .sorted(Comparator.comparing(Categoria::getNome))
                .map(CategoriaMapper::paraResponse)
                .toList();

        return new FilmeResponse(
                filme.getId(),
                filme.getTitulo(),
                filme.getSinopse(),
                filme.getAnoLancamento(),
                filme.getDuracaoMinutos(),
                filme.getPosterUrl(),
                filme.getCriadoEm(),
                categorias,
                arredondar(notaMedia),
                totalAvaliacoes == null ? 0L : totalAvaliacoes);
    }

    /** Uma casa decimal basta para a nota exibida na interface. */
    private static Double arredondar(Double valor) {
        return valor == null ? null : Math.round(valor * 10.0) / 10.0;
    }
}
