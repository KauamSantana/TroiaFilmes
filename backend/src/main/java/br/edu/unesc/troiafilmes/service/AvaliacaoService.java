package br.edu.unesc.troiafilmes.service;

import br.edu.unesc.troiafilmes.dto.request.AvaliacaoRequest;
import br.edu.unesc.troiafilmes.dto.response.AvaliacaoResponse;
import br.edu.unesc.troiafilmes.dto.response.PaginaResponse;
import br.edu.unesc.troiafilmes.entity.Avaliacao;
import br.edu.unesc.troiafilmes.entity.Filme;
import br.edu.unesc.troiafilmes.entity.Perfil;
import br.edu.unesc.troiafilmes.exception.AcessoNegadoException;
import br.edu.unesc.troiafilmes.exception.RecursoNaoEncontradoException;
import br.edu.unesc.troiafilmes.exception.RegraDeNegocioException;
import br.edu.unesc.troiafilmes.mapper.AvaliacaoMapper;
import br.edu.unesc.troiafilmes.repository.AvaliacaoRepository;
import br.edu.unesc.troiafilmes.entity.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Avaliações de filmes feitas pelos perfis.
 *
 * <p>Regras de negócio aplicadas aqui:</p>
 * <ul>
 *   <li>um perfil avalia cada filme uma única vez (regra 1). A tentativa de
 *       repetir resulta em 409, com a orientação de editar a avaliação
 *       existente;</li>
 *   <li>a nota fica entre 1 e 5, validada no DTO e reforçada por constraint
 *       no banco (regra 2);</li>
 *   <li>só o dono do perfil cria, edita ou apaga avaliações dele (regra 4).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacaoRepository;
    private final PerfilService perfilService;
    private final FilmeService filmeService;

    // ------------------------------------------------------------------ leitura

    /** Avaliações públicas de um filme, para a tela de detalhe. */
    @Transactional(readOnly = true)
    public PaginaResponse<AvaliacaoResponse> listarDoFilme(Long filmeId, Pageable pageable) {
        // Confirma que o filme existe para não devolver página vazia de um id inválido.
        filmeService.buscarEntidade(filmeId);

        return PaginaResponse.de(
                avaliacaoRepository.findByFilmeIdOrderByCriadoEmDesc(filmeId, pageable),
                AvaliacaoMapper::paraResponse);
    }

    /** Avaliações feitas por um perfil do usuário autenticado. */
    @Transactional(readOnly = true)
    public List<AvaliacaoResponse> listarDoPerfil(Long perfilId, Usuario autenticado) {
        perfilService.buscarComPosse(perfilId, autenticado);

        return avaliacaoRepository.findByPerfilIdOrderByCriadoEmDesc(perfilId).stream()
                .map(AvaliacaoMapper::paraResponse)
                .toList();
    }

    // ------------------------------------------------------------------ escrita

    /**
     * Registra a avaliação de um filme por um perfil.
     *
     * @throws RegraDeNegocioException se o perfil já tiver avaliado o filme
     * @throws AcessoNegadoException   se o perfil pertencer a outra conta
     */
    @Transactional
    public AvaliacaoResponse criar(Long perfilId, AvaliacaoRequest request,
                                   Usuario autenticado) {
        Perfil perfil = perfilService.buscarComPosse(perfilId, autenticado);
        Filme filme = filmeService.buscarEntidade(request.filmeId());

        if (avaliacaoRepository.existsByPerfilIdAndFilmeId(perfilId, filme.getId())) {
            throw new RegraDeNegocioException(
                    ("O perfil %s já avaliou o filme %s. "
                            + "Edite a avaliação existente para alterar a nota.")
                            .formatted(perfil.getNome(), filme.getTitulo()));
        }

        Avaliacao avaliacao = avaliacaoRepository.save(Avaliacao.builder()
                .perfil(perfil)
                .filme(filme)
                .nota(request.nota())
                .comentario(request.comentario())
                .build());

        log.info("Avaliação registrada: perfil={} filme={} nota={}",
                perfilId, filme.getId(), request.nota());

        return AvaliacaoMapper.paraResponse(avaliacao);
    }

    /**
     * Altera nota e comentário de uma avaliação já feita.
     *
     * <p>O filme não muda: para avaliar outro título, cria-se outra avaliação.</p>
     */
    @Transactional
    public AvaliacaoResponse atualizar(Long avaliacaoId, AvaliacaoRequest request,
                                       Usuario autenticado) {
        Avaliacao avaliacao = buscarComPosse(avaliacaoId, autenticado);

        avaliacao.setNota(request.nota());
        avaliacao.setComentario(request.comentario());

        log.info("Avaliação atualizada: id={} nota={}", avaliacaoId, request.nota());

        return AvaliacaoMapper.paraResponse(avaliacaoRepository.save(avaliacao));
    }

    @Transactional
    public void remover(Long avaliacaoId, Usuario autenticado) {
        Avaliacao avaliacao = buscarComPosse(avaliacaoId, autenticado);

        avaliacaoRepository.delete(avaliacao);
        log.info("Avaliação removida: id={}", avaliacaoId);
    }

    // ----------------------------------------------------------------- privados

    /** Carrega a avaliação garantindo que o perfil dono é do usuário autenticado. */
    private Avaliacao buscarComPosse(Long avaliacaoId, Usuario autenticado) {
        Avaliacao avaliacao = avaliacaoRepository.buscarComRelacionamentos(avaliacaoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Avaliação", avaliacaoId));

        // Reaproveita a checagem de posse do perfil, que já lança 403.
        perfilService.buscarComPosse(avaliacao.getPerfil().getId(), autenticado);

        return avaliacao;
    }
}
