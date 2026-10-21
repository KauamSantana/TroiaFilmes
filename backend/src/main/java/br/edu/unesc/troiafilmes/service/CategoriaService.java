package br.edu.unesc.troiafilmes.service;

import br.edu.unesc.troiafilmes.dto.request.CategoriaRequest;
import br.edu.unesc.troiafilmes.dto.response.CategoriaResponse;
import br.edu.unesc.troiafilmes.entity.Categoria;
import br.edu.unesc.troiafilmes.exception.RecursoNaoEncontradoException;
import br.edu.unesc.troiafilmes.exception.RegraDeNegocioException;
import br.edu.unesc.troiafilmes.mapper.CategoriaMapper;
import br.edu.unesc.troiafilmes.repository.CategoriaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * CRUD das categorias do catálogo. Escrita restrita a ADMIN.
 *
 * <p>Regra de negócio: uma categoria que ainda classifica filmes não pode ser
 * excluída (regra 7), porque isso removeria silenciosamente o vínculo N:N de
 * todos eles.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAllByOrderByNomeAsc().stream()
                .map(CategoriaMapper::paraResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscarPorId(Long id) {
        return CategoriaMapper.paraResponse(buscarEntidade(id));
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest request) {
        String nome = request.nome().trim();

        if (categoriaRepository.existsByNomeIgnoreCase(nome)) {
            throw new RegraDeNegocioException("Já existe uma categoria chamada %s.".formatted(nome));
        }

        Categoria categoria = categoriaRepository.save(Categoria.builder()
                .nome(nome)
                .descricao(request.descricao())
                .build());

        log.info("Categoria criada: id={} nome={}", categoria.getId(), nome);

        return CategoriaMapper.paraResponse(categoria);
    }

    @Transactional
    public CategoriaResponse atualizar(Long id, CategoriaRequest request) {
        Categoria categoria = buscarEntidade(id);
        String nome = request.nome().trim();

        if (categoriaRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraDeNegocioException(
                    "Já existe outra categoria chamada %s.".formatted(nome));
        }

        categoria.setNome(nome);
        categoria.setDescricao(request.descricao());

        log.info("Categoria atualizada: id={}", id);

        return CategoriaMapper.paraResponse(categoriaRepository.save(categoria));
    }

    /**
     * Remove a categoria, desde que nenhum filme dependa dela.
     *
     * @throws RegraDeNegocioException se houver filmes classificados na categoria
     */
    @Transactional
    public void remover(Long id) {
        Categoria categoria = buscarEntidade(id);

        long vinculados = categoriaRepository.contarFilmesVinculados(id);
        if (vinculados > 0) {
            throw new RegraDeNegocioException(
                    ("A categoria %s classifica %d filme(s) e não pode ser excluída. "
                            + "Remova a categoria desses filmes antes.")
                            .formatted(categoria.getNome(), vinculados));
        }

        categoriaRepository.delete(categoria);
        log.info("Categoria removida: id={} nome={}", id, categoria.getNome());
    }

    // -------------------------------------------------- uso por outros services

    @Transactional(readOnly = true)
    public Categoria buscarEntidade(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Categoria", id));
    }

    /**
     * Resolve os ids informados no cadastro de um filme.
     *
     * @throws RecursoNaoEncontradoException se algum dos ids não existir
     */
    @Transactional(readOnly = true)
    public List<Categoria> buscarTodasPorIds(Set<Long> ids) {
        List<Categoria> encontradas = categoriaRepository.findAllById(ids);

        if (encontradas.size() != ids.size()) {
            Set<Long> existentes = encontradas.stream().map(Categoria::getId)
                    .collect(java.util.stream.Collectors.toSet());
            List<Long> ausentes = ids.stream().filter(id -> !existentes.contains(id)).toList();

            throw new RecursoNaoEncontradoException(
                    "Categoria(s) não encontrada(s): %s".formatted(ausentes));
        }

        return encontradas;
    }
}
