package br.edu.unesc.troiafilmes.mapper;

import br.edu.unesc.troiafilmes.dto.response.HistoricoResponse;
import br.edu.unesc.troiafilmes.entity.HistoricoVisualizacao;

public final class HistoricoMapper {

    private HistoricoMapper() {
    }

    public static HistoricoResponse paraResponse(HistoricoVisualizacao historico,
                                                 Double notaMedia, Long totalAvaliacoes) {
        return new HistoricoResponse(
                historico.getId(),
                historico.getPerfil().getId(),
                FilmeMapper.paraResponse(historico.getFilme(), notaMedia, totalAvaliacoes),
                historico.getMinutosAssistidos(),
                historico.getPercentualAssistido(),
                historico.getConcluido(),
                historico.getAssistidoEm());
    }
}
