package br.edu.unesc.troiafilmes.service;

import br.edu.unesc.troiafilmes.dto.request.AtualizarUsuarioRequest;
import br.edu.unesc.troiafilmes.dto.response.PaginaResponse;
import br.edu.unesc.troiafilmes.dto.response.UsuarioResponse;
import br.edu.unesc.troiafilmes.entity.Role;
import br.edu.unesc.troiafilmes.entity.Usuario;
import br.edu.unesc.troiafilmes.exception.RecursoNaoEncontradoException;
import br.edu.unesc.troiafilmes.exception.RegraDeNegocioException;
import br.edu.unesc.troiafilmes.mapper.UsuarioMapper;
import br.edu.unesc.troiafilmes.repository.RefreshTokenRepository;
import br.edu.unesc.troiafilmes.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Administração de contas.
 *
 * <p>As rotas expostas por este service são exclusivas de ADMIN, com uma
 * exceção: {@link #atualizarPropriosDados} e {@link #buscarPorId} também
 * atendem o próprio usuário sobre a sua conta.</p>
 *
 * <p>Regras de negócio aplicadas aqui:</p>
 * <ul>
 *   <li>e-mail é único (regra 9);</li>
 *   <li>a desativação é um soft delete: o registro permanece, mas a conta
 *       deixa de autenticar e tem as sessões encerradas (regra 10);</li>
 *   <li>um administrador não desativa nem rebaixa a si mesmo, para não
 *       deixar o sistema sem administrador ativo.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PaginaResponse<UsuarioResponse> listar(Pageable pageable) {
        return PaginaResponse.de(usuarioRepository.findAll(pageable), UsuarioMapper::paraResponse);
    }

    @Transactional(readOnly = true)
    public UsuarioResponse buscarPorId(Long id) {
        return UsuarioMapper.paraResponse(buscarEntidade(id));
    }

    /** Dados da conta do próprio usuário autenticado. */
    @Transactional(readOnly = true)
    public UsuarioResponse buscarPerfilProprio(Long usuarioId) {
        return UsuarioMapper.paraResponse(buscarEntidade(usuarioId));
    }

    /**
     * Atualiza nome, e-mail e, opcionalmente, a senha da própria conta.
     *
     * <p>Trocar a senha revoga os refresh tokens existentes: sessões antigas
     * não devem sobreviver a uma troca de credencial.</p>
     */
    @Transactional
    public UsuarioResponse atualizarPropriosDados(Long usuarioId, AtualizarUsuarioRequest request) {
        Usuario usuario = buscarEntidade(usuarioId);
        String email = request.email().trim().toLowerCase();

        if (usuarioRepository.existsByEmailAndIdNot(email, usuarioId)) {
            throw new RegraDeNegocioException(
                    "O e-mail %s já está em uso por outra conta.".formatted(email));
        }

        usuario.setNome(request.nome().trim());
        usuario.setEmail(email);

        if (request.novaSenha() != null && !request.novaSenha().isBlank()) {
            usuario.setSenha(passwordEncoder.encode(request.novaSenha()));
            refreshTokenRepository.revogarTodosDoUsuario(usuarioId);
            log.info("Senha alterada e sessões encerradas para o usuário {}", usuarioId);
        }

        log.info("Dados cadastrais atualizados: usuario={}", usuarioId);

        return UsuarioMapper.paraResponse(usuarioRepository.save(usuario));
    }

    /**
     * Ativa ou desativa uma conta (soft delete).
     *
     * @throws RegraDeNegocioException se o administrador tentar desativar a própria conta
     */
    @Transactional
    public UsuarioResponse alterarSituacao(Long id, boolean ativo, Long idDoAdministrador) {
        if (id.equals(idDoAdministrador) && !ativo) {
            throw new RegraDeNegocioException("Você não pode desativar a sua própria conta.");
        }

        Usuario usuario = buscarEntidade(id);
        usuario.setAtivo(ativo);

        if (!ativo) {
            // Sem isso, um token já emitido continuaria valendo até expirar.
            refreshTokenRepository.revogarTodosDoUsuario(id);
        }

        log.info("Conta {} {} pelo administrador {}", id, ativo ? "reativada" : "desativada",
                idDoAdministrador);

        return UsuarioMapper.paraResponse(usuarioRepository.save(usuario));
    }

    /**
     * Promove ou rebaixa o papel de uma conta.
     *
     * @throws RegraDeNegocioException se o administrador tentar rebaixar a si mesmo
     */
    @Transactional
    public UsuarioResponse alterarPapel(Long id, Role role, Long idDoAdministrador) {
        if (id.equals(idDoAdministrador) && role != Role.ADMIN) {
            throw new RegraDeNegocioException("Você não pode remover o seu próprio acesso de administrador.");
        }

        Usuario usuario = buscarEntidade(id);
        usuario.setRole(role);

        // O papel viaja dentro do JWT, então as sessões atuais precisam ser refeitas.
        refreshTokenRepository.revogarTodosDoUsuario(id);

        log.info("Papel da conta {} alterado para {} pelo administrador {}", id, role, idDoAdministrador);

        return UsuarioMapper.paraResponse(usuarioRepository.save(usuario));
    }

    @Transactional(readOnly = true)
    public Usuario buscarEntidade(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Usuário", id));
    }
}
