package br.edu.unesc.troiafilmes.mapper;

import br.edu.unesc.troiafilmes.dto.response.UsuarioResponse;
import br.edu.unesc.troiafilmes.entity.Usuario;

/** Conversão de {@link Usuario} para o DTO de saída. */
public final class UsuarioMapper {

    private UsuarioMapper() {
    }

    public static UsuarioResponse paraResponse(Usuario usuario) {
        return new UsuarioResponse(
                usuario.getId(),
                usuario.getNome(),
                usuario.getEmail(),
                usuario.getRole(),
                usuario.getAtivo(),
                usuario.getCriadoEm());
    }
}
