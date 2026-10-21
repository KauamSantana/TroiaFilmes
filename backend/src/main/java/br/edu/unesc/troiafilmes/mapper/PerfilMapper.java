package br.edu.unesc.troiafilmes.mapper;

import br.edu.unesc.troiafilmes.dto.response.PerfilResponse;
import br.edu.unesc.troiafilmes.entity.Perfil;

public final class PerfilMapper {

    private PerfilMapper() {
    }

    public static PerfilResponse paraResponse(Perfil perfil) {
        return new PerfilResponse(
                perfil.getId(),
                perfil.getUsuario().getId(),
                perfil.getNome(),
                perfil.getAvatarUrl(),
                perfil.getInfantil(),
                perfil.getCriadoEm());
    }
}
