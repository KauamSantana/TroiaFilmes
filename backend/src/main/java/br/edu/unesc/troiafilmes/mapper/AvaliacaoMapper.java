package br.edu.unesc.troiafilmes.mapper;

import br.edu.unesc.troiafilmes.dto.response.AvaliacaoResponse;
import br.edu.unesc.troiafilmes.entity.Avaliacao;

public final class AvaliacaoMapper {

    private AvaliacaoMapper() {
    }

    public static AvaliacaoResponse paraResponse(Avaliacao avaliacao) {
        return new AvaliacaoResponse(
                avaliacao.getId(),
                avaliacao.getPerfil().getId(),
                avaliacao.getPerfil().getNome(),
                avaliacao.getFilme().getId(),
                avaliacao.getFilme().getTitulo(),
                avaliacao.getNota(),
                avaliacao.getComentario(),
                avaliacao.getCriadoEm());
    }
}
