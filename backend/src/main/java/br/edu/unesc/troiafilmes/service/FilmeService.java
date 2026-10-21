package br.edu.unesc.troiafilmes.service;

import br.edu.unesc.troiafilmes.dto.request.FilmeRequest;
import br.edu.unesc.troiafilmes.dto.response.FilmeResponse;
import br.edu.unesc.troiafilmes.dto.response.PaginaResponse;
import br.edu.unesc.troiafilmes.entity.Categoria;
import br.edu.unesc.troiafilmes.entity.Filme;
import br.edu.unesc.troiafilmes.exception.RecursoNaoEncontradoException;
import br.edu.unesc.troiafilmes.exception.RegraDeNegocioException;
import br.edu.unesc.troiafilmes.mapper.FilmeMapper;
import br.edu.unesc.troiafilmes.repository.AvaliacaoRepository;
import br.edu.unesc.troiafilmes.repository.FilmeRepository;
import br.edu.unesc.troiafilmes.repository.projection.ResumoAvaliacaoProjection;
import br.edu.unesc.troiafilmes.repository.specification.FilmeSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * CRUD do catálogo e busca paginada. Escrita restrita a ADMIN.
 *
 * <p>Regras de negócio aplicadas aqui:</p>
 * <ul>
 *   <li>nota média e total de avaliações vêm de consulta de agregação, nunca
 *       de coluna redundante (regra 5);</li>
 *   <li>busca combinável por título, categoria e ano, sempre paginada (regra 11);</li>
 *   <li>não se cadastra o mesmo título duas vezes no mesmo ano.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FilmeService {

    private final FilmeRepository filmeRepository;
    private final AvaliacaoRepository avaliacaoRepository;
    private final CategoriaService categoriaService;
    private final ArmazenamentoService armazenamentoService;

    // ------------------------------------------------------------------ leitura

    /**
     * Busca paginada no catálogo, com filtros opcionais e combináveis.
     *
     * <p>Roda em três consultas de custo fixo, independentemente do tamanho
     * da página: a página de filmes, as categorias daqueles ids e o resumo
     * das avaliações daqueles ids. É o que evita o N+1 que apareceria ao
     * navegar pelas coleções item a item.</p>
     */
    @Transactional(readOnly = true)
    public PaginaResponse<FilmeResponse> buscar(String titulo, Long categoriaId,
                                                Integer ano, Pageable pageable) {

        Page<Filme> pagina = filmeRepository.findAll(
                FilmeSpecification.comFiltros(titulo, categoriaId, ano), pageable);

        List<Long> ids = pagina.getContent().stream().map(Filme::getId).toList();

        if (ids.isEmpty()) {
            return PaginaResponse.de(pagina, List.<FilmeResponse>of());
        }

        // Traz as categorias da página para o contexto de persistência.
        filmeRepository.hidratarCategorias(ids);

        Map<Long, ResumoAvaliacaoProjection> resumos = resumirAvaliacoes(ids);

        List<FilmeResponse> conteudo = pagina.getContent().stream()
                .map(filme -> montarResposta(filme, resumos.get(filme.getId())))
                .toList();

        return PaginaResponse.de(pagina, conteudo);
    }

    @Transactional(readOnly = true)
    public FilmeResponse buscarPorId(Long id) {
        Filme filme = filmeRepository.buscarComCategorias(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Filme", id));

        return montarResposta(filme, resumirAvaliacoes(List.of(id)).get(id));
    }

    // ------------------------------------------------------------------ escrita

    @Transactional
    public FilmeResponse criar(FilmeRequest request) {
        String titulo = request.titulo().trim();

        if (filmeRepository.existsByTituloIgnoreCaseAndAnoLancamento(titulo, request.anoLancamento())) {
            throw new RegraDeNegocioException(
                    "O filme %s (%d) já está no catálogo.".formatted(titulo, request.anoLancamento()));
        }

        Filme filme = Filme.builder()
                .titulo(titulo)
                .sinopse(request.sinopse())
                .anoLancamento(request.anoLancamento())
                .duracaoMinutos(request.duracaoMinutos())
                .posterUrl(request.posterUrl())
                .build();

        aplicarCategorias(filme, request.categoriaIds());

        filme = filmeRepository.save(filme);
        log.info("Filme cadastrado: id={} titulo={}", filme.getId(), titulo);

        return montarResposta(filme, null);
    }

    @Transactional
    public FilmeResponse atualizar(Long id, FilmeRequest request) {
        Filme filme = filmeRepository.buscarComCategorias(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Filme", id));

        String titulo = request.titulo().trim();

        if (filmeRepository.existsByTituloIgnoreCaseAndAnoLancamentoAndIdNot(
                titulo, request.anoLancamento(), id)) {
            throw new RegraDeNegocioException(
                    "Outro filme com o título %s (%d) já está no catálogo."
                            .formatted(titulo, request.anoLancamento()));
        }

        String posterAnterior = filme.getPosterUrl();

        filme.setTitulo(titulo);
        filme.setSinopse(request.sinopse());
        filme.setAnoLancamento(request.anoLancamento());
        filme.setDuracaoMinutos(request.duracaoMinutos());
        filme.setPosterUrl(request.posterUrl());

        filme.getCategorias().clear();
        aplicarCategorias(filme, request.categoriaIds());

        filme = filmeRepository.save(filme);

        // Trocar o pôster por outro endereço deixaria o arquivo enviado antes
        // ocupando disco sem que nada mais aponte para ele.
        if (!Objects.equals(posterAnterior, filme.getPosterUrl())) {
            armazenamentoService.removerSeForNosso(posterAnterior);
        }

        log.info("Filme atualizado: id={}", id);

        return montarResposta(filme, resumirAvaliacoes(List.of(id)).get(id));
    }

    /**
     * Remove o filme do catálogo.
     *
     * <p>As avaliações, itens de lista e registros de histórico associados
     * caem junto, por cascata declarada nas FKs.</p>
     */
    @Transactional
    public void remover(Long id) {
        Filme filme = filmeRepository.buscarComCategorias(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Filme", id));

        filme.limparCategorias();
        filmeRepository.delete(filme);
        armazenamentoService.removerSeForNosso(filme.getPosterUrl());

        log.info("Filme removido: id={} titulo={}", id, filme.getTitulo());
    }

    // ------------------------------------------------------------------ pôster

    /**
     * Substitui o pôster do filme por uma imagem enviada.
     *
     * <p>O arquivo antigo é apagado depois que o novo está gravado e o filme
     * atualizado — nessa ordem, para que uma falha no meio do caminho deixe o
     * filme com a imagem velha em vez de com nenhuma.</p>
     */
    @Transactional
    public FilmeResponse definirPoster(Long id, MultipartFile imagem) {
        Filme filme = filmeRepository.buscarComCategorias(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Filme", id));

        String anterior = filme.getPosterUrl();

        filme.setPosterUrl(armazenamentoService.salvarImagem(imagem));
        filme = filmeRepository.save(filme);

        armazenamentoService.removerSeForNosso(anterior);
        log.info("Pôster atualizado: filme={} url={}", id, filme.getPosterUrl());

        return montarResposta(filme, resumirAvaliacoes(List.of(id)).get(id));
    }

    /** Remove o pôster, devolvendo o filme ao cartão com degradê e título. */
    @Transactional
    public FilmeResponse removerPoster(Long id) {
        Filme filme = filmeRepository.buscarComCategorias(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Filme", id));

        String anterior = filme.getPosterUrl();
        filme.setPosterUrl(null);
        filme = filmeRepository.save(filme);

        armazenamentoService.removerSeForNosso(anterior);
        log.info("Pôster removido do filme {}", id);

        return montarResposta(filme, resumirAvaliacoes(List.of(id)).get(id));
    }

    // ----------------------------------------------- uso por outros services

    @Transactional(readOnly = true)
    public Filme buscarEntidade(Long id) {
        return filmeRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Filme", id));
    }

    /**
     * Resumo de avaliações indexado por id de filme, para montar respostas em
     * lote sem uma consulta por item.
     */
    @Transactional(readOnly = true)
    public Map<Long, ResumoAvaliacaoProjection> resumirAvaliacoes(Collection<Long> filmeIds) {
        if (filmeIds == null || filmeIds.isEmpty()) {
            return Map.of();
        }
        return avaliacaoRepository.resumirPorFilmes(filmeIds).stream()
                .collect(Collectors.toMap(ResumoAvaliacaoProjection::getFilmeId, Function.identity()));
    }

    /** Converte a entidade em DTO anexando o resumo de avaliações, quando houver. */
    public FilmeResponse montarResposta(Filme filme, ResumoAvaliacaoProjection resumo) {
        return resumo == null
                ? FilmeMapper.paraResponse(filme, null, 0L)
                : FilmeMapper.paraResponse(filme, resumo.getNotaMedia(), resumo.getTotalAvaliacoes());
    }

    // ----------------------------------------------------------------- privados

    private void aplicarCategorias(Filme filme, Set<Long> categoriaIds) {
        List<Categoria> categorias = categoriaService.buscarTodasPorIds(categoriaIds);
        filme.getCategorias().addAll(categorias);
    }
}
