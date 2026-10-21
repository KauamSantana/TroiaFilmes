package br.edu.unesc.troiafilmes.service;

import br.edu.unesc.troiafilmes.dto.request.PerfilRequest;
import br.edu.unesc.troiafilmes.dto.response.PerfilResponse;
import br.edu.unesc.troiafilmes.entity.Perfil;
import br.edu.unesc.troiafilmes.entity.Usuario;
import br.edu.unesc.troiafilmes.exception.AcessoNegadoException;
import br.edu.unesc.troiafilmes.exception.RecursoNaoEncontradoException;
import br.edu.unesc.troiafilmes.exception.RegraDeNegocioException;
import br.edu.unesc.troiafilmes.mapper.PerfilMapper;
import br.edu.unesc.troiafilmes.repository.PerfilRepository;
import br.edu.unesc.troiafilmes.repository.UsuarioRepository;
import br.edu.unesc.troiafilmes.entity.Usuario;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CRUD dos perfis de consumo e ponto central da checagem de posse.
 *
 * <p>Os demais services que operam sobre dados de um perfil (avaliações,
 * Minha Lista e histórico) chamam {@link #buscarComPosse} antes de qualquer
 * operação, de modo que a regra fique escrita em um lugar só.</p>
 *
 * <p>Regras de negócio aplicadas aqui:</p>
 * <ul>
 *   <li>a quantidade de perfis é limitada pelo plano da assinatura ativa (regra 3);</li>
 *   <li>um perfil só é acessível pelo dono da conta (regra 4);</li>
 *   <li>o último perfil da conta não pode ser excluído.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PerfilService {

    private final PerfilRepository perfilRepository;
    private final UsuarioRepository usuarioRepository;
    private final AssinaturaService assinaturaService;

    @Transactional(readOnly = true)
    public List<PerfilResponse> listarDoUsuario(Long usuarioId) {
        return perfilRepository.findByUsuarioIdOrderByCriadoEmAsc(usuarioId).stream()
                .map(PerfilMapper::paraResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PerfilResponse buscarPorId(Long perfilId, Usuario autenticado) {
        return PerfilMapper.paraResponse(buscarComPosse(perfilId, autenticado));
    }

    /**
     * Cria um perfil na conta do usuário autenticado.
     *
     * @throws RegraDeNegocioException se o limite do plano já tiver sido atingido
     *                                 ou se já existir um perfil com o mesmo nome
     */
    @Transactional
    public PerfilResponse criar(PerfilRequest request, Usuario autenticado) {
        Long usuarioId = autenticado.getId();
        String nome = request.nome().trim();

        validarLimiteDoPlano(usuarioId);

        if (perfilRepository.existsByUsuarioIdAndNomeIgnoreCase(usuarioId, nome)) {
            throw new RegraDeNegocioException(
                    "Você já possui um perfil chamado %s.".formatted(nome));
        }

        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Usuário", usuarioId));

        Perfil perfil = perfilRepository.save(Perfil.builder()
                .usuario(usuario)
                .nome(nome)
                .avatarUrl(request.avatarUrl())
                .infantil(request.infantil() != null && request.infantil())
                .build());

        log.info("Perfil criado: id={} usuario={}", perfil.getId(), usuarioId);

        return PerfilMapper.paraResponse(perfil);
    }

    @Transactional
    public PerfilResponse atualizar(Long perfilId, PerfilRequest request,
                                    Usuario autenticado) {
        Perfil perfil = buscarComPosse(perfilId, autenticado);
        String nome = request.nome().trim();

        if (perfilRepository.existsByUsuarioIdAndNomeIgnoreCaseAndIdNot(
                perfil.getUsuario().getId(), nome, perfilId)) {
            throw new RegraDeNegocioException(
                    "Você já possui outro perfil chamado %s.".formatted(nome));
        }

        perfil.setNome(nome);
        perfil.setAvatarUrl(request.avatarUrl());
        if (request.infantil() != null) {
            perfil.setInfantil(request.infantil());
        }

        log.info("Perfil atualizado: id={}", perfilId);

        return PerfilMapper.paraResponse(perfilRepository.save(perfil));
    }

    /**
     * Exclui o perfil e, em cascata, suas avaliações, itens de lista e
     * histórico.
     */
    @Transactional
    public void remover(Long perfilId, Usuario autenticado) {
        Perfil perfil = buscarComPosse(perfilId, autenticado);
        Long usuarioId = perfil.getUsuario().getId();

        if (perfilRepository.countByUsuarioId(usuarioId) <= 1) {
            throw new RegraDeNegocioException(
                    "A conta precisa manter ao menos um perfil. Crie outro antes de excluir este.");
        }

        perfilRepository.delete(perfil);
        log.info("Perfil removido: id={} usuario={}", perfilId, usuarioId);
    }

    // ------------------------------------------------------ posse e limites

    /**
     * Carrega o perfil garantindo que ele pertence ao usuário autenticado.
     *
     * <p>É a segunda camada de autorização do sistema: o papel define quais
     * rotas a pessoa alcança, esta checagem define quais registros ela pode
     * tocar dentro delas. Vale inclusive para ADMIN, porque perfil, histórico
     * e avaliações são dados pessoais do assinante, não itens de catálogo.</p>
     *
     * @throws RecursoNaoEncontradoException se o perfil não existir
     * @throws AcessoNegadoException         se o perfil for de outra conta
     */
    @Transactional(readOnly = true)
    public Perfil buscarComPosse(Long perfilId, Usuario autenticado) {
        Perfil perfil = perfilRepository.buscarComUsuario(perfilId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Perfil", perfilId));

        if (!perfil.getUsuario().getId().equals(autenticado.getId())) {
            log.warn("Usuário {} tentou acessar o perfil {} de outra conta",
                    autenticado.getId(), perfilId);
            throw AcessoNegadoException.doRecurso("Este perfil");
        }

        return perfil;
    }

    private void validarLimiteDoPlano(Long usuarioId) {
        int limite = assinaturaService.limiteDePerfis(usuarioId);
        long emUso = perfilRepository.countByUsuarioId(usuarioId);

        if (emUso >= limite) {
            throw new RegraDeNegocioException(
                    ("O seu plano (%s) permite no máximo %d perfil(is) e você já possui %d. "
                            + "Faça upgrade do plano para criar mais perfis.")
                            .formatted(assinaturaService.nomeDoPlanoVigente(usuarioId), limite, emUso));
        }
    }
}
