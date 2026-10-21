package br.edu.unesc.troiafilmes.mapper;

import br.edu.unesc.troiafilmes.dto.response.CategoriaResponse;
import br.edu.unesc.troiafilmes.entity.Categoria;

public final class CategoriaMapper {

    private CategoriaMapper() {
    }

    public static CategoriaResponse paraResponse(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNome(),
                categoria.getDescricao());
    }
}
