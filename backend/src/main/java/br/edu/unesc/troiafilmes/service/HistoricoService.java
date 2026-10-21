package br.edu.unesc.troiafilmes.service;

import br.edu.unesc.troiafilmes.dto.request.ProgressoRequest;
import br.edu.unesc.troiafilmes.dto.response.HistoricoResponse;
import br.edu.unesc.troiafilmes.entity.Filme;
import br.edu.unesc.troiafilmes.entity.HistoricoVisualizacao;
import br.edu.unesc.troiafilmes.entity.Perfil;
import br.edu.unesc.troiafilmes.exception.RecursoNaoEncontradoException;
import br.edu.unesc.troiafilmes.exception.RegraDeNegocioException;
import br.edu.unesc.troiafilmes.mapper.HistoricoMapper;
import br.edu.unesc.troiafilmes.repository.HistoricoVisualizacaoRepository;
import br.edu.unesc.troiafilmes.repository.projection.ResumoAvaliacaoProjection;
import br.edu.unesc.troiafilmes.entity.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Histórico de visualização e prateleira "Continuar assistindo".
 *
 * <p>Regras de negócio aplicadas aqui:</p>
 * <ul>
 *   <li>existe um único registro por par perfil/filme: assistir de novo
 *       atualiza o progresso em vez de criar outra linha;</li>
 *   <li>o filme é marcado como concluído ao atingir 90% da duração (regra 8);</li>
 *   <li>o progresso informado não pode ultrapassar a duração do filme;</li>
 *   <li>só o dono do perfil enxerga e altera o próprio histórico (regra 4).</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HistoricoService {

    private final HistoricoVisualizacaoRepository historicoRepository;
    private final PerfilService perfilService;
    private final FilmeService filmeService;

    @Transactional(readOnly = true)
    public List<HistoricoResponse> listar(Long perfilId, Usuario autenticado) {
        perfilService.buscarComPosse(perfilId, autenticado);
        return montarLista(historicoRepository.listarPorPerfil(perfilId));
    }

    /** O que o perfil começou e ainda não terminou de assistir. */
    @Transactional(readOnly = true)
    public List<HistoricoResponse> continuarAssistindo(Long perfilId, Usuario autenticado) {
        perfilService.buscarComPosse(perfilId, autenticado);
        return montarLista(historicoRepository.listarEmAndamento(perfilId));
    }

    /**
     * Registra a posição de reprodução, criando o registro na primeira vez.
     *
     * @throws RegraDeNegocioException se os minutos informados excederem a
     *                                 duração do filme
     */
    @Transactional
    public HistoricoResponse registrarProgresso(Long perfilId, ProgressoRequest request,
                                                Usuario autenticado) {
        Perfil perfil = perfilService.buscarComPosse(perfilId, autenticado);
        Filme filme = filmeService.buscarEntidade(request.filmeId());

        int minutos = request.minutosAssistidos();

        if (minutos > filme.getDuracaoMinutos()) {
            throw new RegraDeNegocioException(
                    "O filme %s tem %d minutos; não é possível registrar %d minutos assistidos."
                            .formatted(filme.getTitulo(), filme.getDuracaoMinutos(), minutos));
        }

        HistoricoVisualizacao historico = historicoRepository
                .findByPerfilIdAndFilmeId(perfilId, filme.getId())
                .orElseGet(() -> HistoricoVisualizacao.builder()
                        .perfil(perfil)
                        .filme(filme)
                        .build());

        historico.registrarProgresso(minutos, filme.getDuracaoMinutos());
        historico = historicoRepository.save(historico);

        log.info("Progresso registrado: perfil={} filme={} minutos={} concluido={}",
                perfilId, filme.getId(), minutos, historico.getConcluido());

        Map<Long, ResumoAvaliacaoProjection> resumos =
                filmeService.resumirAvaliacoes(List.of(filme.getId()));

        return montar(historico, resumos);
    }

    /** Remove um filme do histórico do perfil. */
    @Transactional
    public void remover(Long perfilId, Long filmeId, Usuario autenticado) {
        perfilService.buscarComPosse(perfilId, autenticado);

        HistoricoVisualizacao historico = historicoRepository
                .findByPerfilIdAndFilmeId(perfilId, filmeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Este filme não está no histórico do perfil informado."));

        historicoRepository.delete(historico);
        log.info("Histórico removido: perfil={} filme={}", perfilId, filmeId);
    }

    // ----------------------------------------------------------------- privados

    private List<HistoricoResponse> montarLista(List<HistoricoVisualizacao> registros) {
        Map<Long, ResumoAvaliacaoProjection> resumos = filmeService.resumirAvaliacoes(
                registros.stream().map(h -> h.getFilme().getId()).toList());

        return registros.stream().map(h -> montar(h, resumos)).toList();
    }

    private HistoricoResponse montar(HistoricoVisualizacao historico,
                                     Map<Long, ResumoAvaliacaoProjection> resumos) {
        ResumoAvaliacaoProjection resumo = resumos.get(historico.getFilme().getId());

        return resumo == null
                ? HistoricoMapper.paraResponse(historico, null, 0L)
                : HistoricoMapper.paraResponse(historico, resumo.getNotaMedia(), resumo.getTotalAvaliacoes());
    }
}
