package br.edu.unesc.troiafilmes.mapper;

import br.edu.unesc.troiafilmes.dto.response.PlanoResponse;
import br.edu.unesc.troiafilmes.entity.Plano;

public final class PlanoMapper {

    private PlanoMapper() {
    }

    public static PlanoResponse paraResponse(Plano plano) {
        return new PlanoResponse(
                plano.getId(),
                plano.getNome(),
                plano.getDescricao(),
                plano.getPreco(),
                plano.getMaxPerfis(),
                plano.getQualidadeMaxima(),
                plano.getAtivo());
    }
}
