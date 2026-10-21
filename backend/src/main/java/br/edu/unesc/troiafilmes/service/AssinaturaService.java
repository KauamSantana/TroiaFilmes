package br.edu.unesc.troiafilmes.service;

import br.edu.unesc.troiafilmes.dto.response.AssinaturaResponse;
import br.edu.unesc.troiafilmes.entity.Assinatura;
import br.edu.unesc.troiafilmes.entity.Plano;
import br.edu.unesc.troiafilmes.entity.StatusAssinatura;
import br.edu.unesc.troiafilmes.entity.Usuario;
import br.edu.unesc.troiafilmes.exception.RecursoNaoEncontradoException;
import br.edu.unesc.troiafilmes.exception.RegraDeNegocioException;
import br.edu.unesc.troiafilmes.mapper.AssinaturaMapper;
import br.edu.unesc.troiafilmes.repository.AssinaturaRepository;
import br.edu.unesc.troiafilmes.repository.PerfilRepository;
import br.edu.unesc.troiafilmes.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Contratação e cancelamento de planos.
 *
 * <p>Regras de negócio aplicadas aqui:</p>
 * <ul>
 *   <li>um usuário tem no máximo uma assinatura ATIVA; contratar outro plano
 *       cancela a anterior automaticamente (regra 6);</li>
 *   <li>não é possível migrar para um plano que comporte menos perfis do que
 *       a conta já usa, o que deixaria perfis órfãos;</li>
 *   <li>plano desativado não aceita novas contratações.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssinaturaService {

    private final AssinaturaRepository assinaturaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilRepository perfilRepository;
    private final PlanoService planoService;

    /** Assinatura vigente do usuário autenticado. */
    @Transactional(readOnly = true)
    public AssinaturaResponse buscarAtiva(Long usuarioId) {
        return assinaturaRepository.buscarAtivaComPlano(usuarioId)
                .map(AssinaturaMapper::paraResponse)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Este usuário não possui assinatura ativa."));
    }

    @Transactional(readOnly = true)
    public List<AssinaturaResponse> listarHistorico(Long usuarioId) {
        return assinaturaRepository.listarHistorico(usuarioId).stream()
                .map(AssinaturaMapper::paraResponse)
                .toList();
    }

    /**
     * Contrata um plano para o usuário, encerrando a assinatura anterior.
     *
     * @throws RegraDeNegocioException se o plano estiver desativado, já for o
     *                                 plano vigente, ou comportar menos perfis
     *                                 do que a conta já possui
     */
    @Transactional
    public AssinaturaResponse contratar(Long usuarioId, Long planoId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Usuário", usuarioId));

        Plano novoPlano = planoService.buscarEntidade(planoId);

        if (Boolean.FALSE.equals(novoPlano.getAtivo())) {
            throw new RegraDeNegocioException(
                    "O plano %s não está disponível para contratação.".formatted(novoPlano.getNome()));
        }

        Optional<Assinatura> vigente = assinaturaRepository.buscarAtivaComPlano(usuarioId);

        if (vigente.isPresent() && vigente.get().getPlano().getId().equals(planoId)) {
            throw new RegraDeNegocioException(
                    "O plano %s já é a sua assinatura atual.".formatted(novoPlano.getNome()));
        }

        validarCapacidadeDePerfis(usuarioId, novoPlano);

        // Só pode existir uma ATIVA por usuário, então a anterior encerra aqui.
        //
        // O flush é obrigatório, e não um detalhe de estilo: como o id é
        // GenerationType.IDENTITY, o save() da nova assinatura dispara o INSERT
        // na hora, para obter o id gerado. Sem forçar o UPDATE antes, as duas
        // linhas ficariam ATIVA no mesmo instante e o índice único parcial
        // uk_assinatura_ativa_por_usuario rejeitaria a operação.
        vigente.ifPresent(anterior -> {
            anterior.cancelar();
            assinaturaRepository.saveAndFlush(anterior);
            log.info("Assinatura {} cancelada por troca de plano", anterior.getId());
        });

        Assinatura nova = assinaturaRepository.save(Assinatura.builder()
                .usuario(usuario)
                .plano(novoPlano)
                .status(StatusAssinatura.ATIVA)
                .build());

        log.info("Usuário {} contratou o plano {}", usuarioId, novoPlano.getNome());

        return AssinaturaMapper.paraResponse(nova);
    }

    /** Cancela a assinatura vigente do usuário. */
    @Transactional
    public AssinaturaResponse cancelar(Long usuarioId) {
        Assinatura vigente = assinaturaRepository.buscarAtivaComPlano(usuarioId)
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Este usuário não possui assinatura ativa para cancelar."));

        vigente.cancelar();
        log.info("Assinatura {} cancelada pelo usuário {}", vigente.getId(), usuarioId);

        return AssinaturaMapper.paraResponse(assinaturaRepository.save(vigente));
    }

    /**
     * Limite de perfis do usuário, derivado do plano vigente.
     *
     * <p>Sem assinatura ativa a conta fica com direito a um único perfil, o
     * mesmo do plano de entrada.</p>
     */
    @Transactional(readOnly = true)
    public int limiteDePerfis(Long usuarioId) {
        return assinaturaRepository.buscarAtivaComPlano(usuarioId)
                .map(assinatura -> assinatura.getPlano().getMaxPerfis())
                .orElse(1);
    }

    /** Nome do plano vigente, usado nas mensagens de erro. */
    @Transactional(readOnly = true)
    public String nomeDoPlanoVigente(Long usuarioId) {
        return assinaturaRepository.buscarAtivaComPlano(usuarioId)
                .map(assinatura -> assinatura.getPlano().getNome())
                .orElse("sem assinatura ativa");
    }

    private void validarCapacidadeDePerfis(Long usuarioId, Plano novoPlano) {
        long perfisEmUso = perfilRepository.countByUsuarioId(usuarioId);

        if (perfisEmUso > novoPlano.getMaxPerfis()) {
            throw new RegraDeNegocioException(
                    ("Você possui %d perfis e o plano %s permite no máximo %d. "
                            + "Exclua %d perfil(is) antes de migrar.")
                            .formatted(perfisEmUso, novoPlano.getNome(), novoPlano.getMaxPerfis(),
                                    perfisEmUso - novoPlano.getMaxPerfis()));
        }
    }
}
