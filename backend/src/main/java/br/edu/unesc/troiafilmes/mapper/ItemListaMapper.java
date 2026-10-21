package br.edu.unesc.troiafilmes.mapper;

import br.edu.unesc.troiafilmes.dto.response.ItemListaResponse;
import br.edu.unesc.troiafilmes.entity.ItemLista;

public final class ItemListaMapper {

    private ItemListaMapper() {
    }

    public static ItemListaResponse paraResponse(ItemLista item, Double notaMedia, Long totalAvaliacoes) {
        return new ItemListaResponse(
                item.getId(),
                item.getPerfil().getId(),
                FilmeMapper.paraResponse(item.getFilme(), notaMedia, totalAvaliacoes),
                item.getAdicionadoEm());
    }
}
