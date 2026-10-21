package br.edu.unesc.troiafilmes.service;

import br.edu.unesc.troiafilmes.dto.request.PlanoRequest;
import br.edu.unesc.troiafilmes.dto.response.PlanoResponse;
import br.edu.unesc.troiafilmes.entity.Plano;
import br.edu.unesc.troiafilmes.entity.StatusAssinatura;
import br.edu.unesc.troiafilmes.exception.RecursoNaoEncontradoException;
import br.edu.unesc.troiafilmes.exception.RegraDeNegocioException;
import br.edu.unesc.troiafilmes.mapper.PlanoMapper;
import br.edu.unesc.troiafilmes.repository.AssinaturaRepository;
import br.edu.unesc.troiafilmes.repository.PlanoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD dos planos de assinatura. Escrita restrita a ADMIN.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PlanoService {

    private final PlanoRepository planoRepository;
    private final AssinaturaRepository assinaturaRepository;

    @Transactional(readOnly = true)
    public List<PlanoResponse> listar(boolean apenasAtivos) {
        List<Plano> planos = apenasAtivos
                ? planoRepository.findByAtivoTrueOrderByPrecoAsc()
                : planoRepository.findAllByOrderByPrecoAsc();

        return planos.stream().map(PlanoMapper::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public PlanoResponse buscarPorId(Long id) {
        return PlanoMapper.paraResponse(buscarEntidade(id));
    }

    @Transactional
    public PlanoResponse criar(PlanoRequest request) {
        if (planoRepository.existsByNomeIgnoreCase(request.nome().trim())) {
            throw new RegraDeNegocioException(
                    "Já existe um plano chamado %s.".formatted(request.nome().trim()));
        }

        Plano plano = Plano.builder()
                .nome(request.nome().trim())
                .descricao(request.descricao())
                .preco(request.preco())
                .maxPerfis(request.maxPerfis())
                .qualidadeMaxima(request.qualidadeMaxima())
                .ativo(request.ativo() == null || request.ativo())
                .build();

        plano = planoRepository.save(plano);
        log.info("Plano criado: id={} nome={}", plano.getId(), plano.getNome());

        return PlanoMapper.paraResponse(plano);
    }

    @Transactional
    public PlanoResponse atualizar(Long id, PlanoRequest request) {
        Plano plano = buscarEntidade(id);
        String nome = request.nome().trim();

        if (planoRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new RegraDeNegocioException("Já existe outro plano chamado %s.".formatted(nome));
        }

        plano.setNome(nome);
        plano.setDescricao(request.descricao());
        plano.setPreco(request.preco());
        plano.setMaxPerfis(request.maxPerfis());
        plano.setQualidadeMaxima(request.qualidadeMaxima());
        if (request.ativo() != null) {
            plano.setAtivo(request.ativo());
        }

        log.info("Plano atualizado: id={}", id);
        return PlanoMapper.paraResponse(planoRepository.save(plano));
    }

    /**
     * Remove um plano.
     *
     * <p>Um plano com assinantes ativos não é excluído: apagá-lo deixaria
     * essas contas sem limite de perfis definido. Nesse caso, a saída correta
     * é desativá-lo, o que impede novas contratações sem afetar quem já
     * assinou.</p>
     */
    @Transactional
    public void remover(Long id) {
        Plano plano = buscarEntidade(id);

        long assinantes = assinaturaRepository.countByPlanoIdAndStatus(id, StatusAssinatura.ATIVA);
        if (assinantes > 0) {
            throw new RegraDeNegocioException(
                    ("O plano %s possui %d assinatura(s) ativa(s) e não pode ser excluído. "
                            + "Desative-o para impedir novas contratações.")
                            .formatted(plano.getNome(), assinantes));
        }

        planoRepository.delete(plano);
        log.info("Plano removido: id={} nome={}", id, plano.getNome());
    }

    /** Busca a entidade para uso interno de outros services. */
    @Transactional(readOnly = true)
    public Plano buscarEntidade(Long id) {
        return planoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Plano", id));
    }
}
