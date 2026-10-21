package br.edu.unesc.troiafilmes.service;

import br.edu.unesc.troiafilmes.dto.response.ItemListaResponse;
import br.edu.unesc.troiafilmes.entity.Filme;
import br.edu.unesc.troiafilmes.entity.ItemLista;
import br.edu.unesc.troiafilmes.entity.Perfil;
import br.edu.unesc.troiafilmes.exception.RecursoNaoEncontradoException;
import br.edu.unesc.troiafilmes.exception.RegraDeNegocioException;
import br.edu.unesc.troiafilmes.mapper.ItemListaMapper;
import br.edu.unesc.troiafilmes.repository.ItemListaRepository;
import br.edu.unesc.troiafilmes.repository.projection.ResumoAvaliacaoProjection;
import br.edu.unesc.troiafilmes.entity.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * "Minha Lista": os filmes que um perfil salvou para assistir depois.
 *
 * <p>É o segundo relacionamento N:N do sistema (Perfil x Filme), aqui com o
 * atributo {@code adicionadoEm} preservando a ordem de inclusão.</p>
 *
 * <p>Regra de negócio: o mesmo filme não entra duas vezes na lista do mesmo
 * perfil, e só o dono da conta mexe na própria lista (regra 4).</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MinhaListaService {

    private final ItemListaRepository itemListaRepository;
    private final PerfilService perfilService;
    private final FilmeService filmeService;

    @Transactional(readOnly = true)
    public List<ItemListaResponse> listar(Long perfilId, Usuario autenticado) {
        perfilService.buscarComPosse(perfilId, autenticado);

        List<ItemLista> itens = itemListaRepository.listarPorPerfil(perfilId);

        Map<Long, ResumoAvaliacaoProjection> resumos = filmeService.resumirAvaliacoes(
                itens.stream().map(item -> item.getFilme().getId()).toList());

        return itens.stream()
                .map(item -> montar(item, resumos))
                .toList();
    }

    /**
     * Adiciona um filme à lista do perfil.
     *
     * @throws RegraDeNegocioException se o filme já estiver na lista
     */
    @Transactional
    public ItemListaResponse adicionar(Long perfilId, Long filmeId, Usuario autenticado) {
        Perfil perfil = perfilService.buscarComPosse(perfilId, autenticado);
        Filme filme = filmeService.buscarEntidade(filmeId);

        if (itemListaRepository.existsByPerfilIdAndFilmeId(perfilId, filmeId)) {
            throw new RegraDeNegocioException(
                    "O filme %s já está na lista do perfil %s."
                            .formatted(filme.getTitulo(), perfil.getNome()));
        }

        ItemLista item = itemListaRepository.save(ItemLista.builder()
                .perfil(perfil)
                .filme(filme)
                .build());

        log.info("Filme {} adicionado à lista do perfil {}", filmeId, perfilId);

        Map<Long, ResumoAvaliacaoProjection> resumos = filmeService.resumirAvaliacoes(List.of(filmeId));
        return montar(item, resumos);
    }

    /**
     * Remove um filme da lista do perfil.
     *
     * @throws RecursoNaoEncontradoException se o filme não estiver na lista
     */
    @Transactional
    public void remover(Long perfilId, Long filmeId, Usuario autenticado) {
        perfilService.buscarComPosse(perfilId, autenticado);

        ItemLista item = itemListaRepository.findByPerfilIdAndFilmeId(perfilId, filmeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Este filme não está na lista do perfil informado."));

        itemListaRepository.delete(item);
        log.info("Filme {} removido da lista do perfil {}", filmeId, perfilId);
    }

    /** Indica se um filme já está na lista, para o botão da tela de detalhe. */
    @Transactional(readOnly = true)
    public boolean contem(Long perfilId, Long filmeId, Usuario autenticado) {
        perfilService.buscarComPosse(perfilId, autenticado);
        return itemListaRepository.existsByPerfilIdAndFilmeId(perfilId, filmeId);
    }

    private ItemListaResponse montar(ItemLista item, Map<Long, ResumoAvaliacaoProjection> resumos) {
        ResumoAvaliacaoProjection resumo = resumos.get(item.getFilme().getId());

        return resumo == null
                ? ItemListaMapper.paraResponse(item, null, 0L)
                : ItemListaMapper.paraResponse(item, resumo.getNotaMedia(), resumo.getTotalAvaliacoes());
    }
}
