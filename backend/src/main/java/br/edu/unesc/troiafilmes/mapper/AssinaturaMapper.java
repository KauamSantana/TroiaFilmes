package br.edu.unesc.troiafilmes.mapper;

import br.edu.unesc.troiafilmes.dto.response.AssinaturaResponse;
import br.edu.unesc.troiafilmes.entity.Assinatura;

public final class AssinaturaMapper {

    private AssinaturaMapper() {
    }

    public static AssinaturaResponse paraResponse(Assinatura assinatura) {
        return new AssinaturaResponse(
                assinatura.getId(),
                assinatura.getUsuario().getId(),
                PlanoMapper.paraResponse(assinatura.getPlano()),
                assinatura.getDataInicio(),
                assinatura.getDataFim(),
                assinatura.getStatus());
    }
}
