package br.edu.unesc.troiafilmes.service;

import br.edu.unesc.troiafilmes.dto.request.PerfilRequest;
import br.edu.unesc.troiafilmes.entity.Perfil;
import br.edu.unesc.troiafilmes.entity.Role;
import br.edu.unesc.troiafilmes.entity.Usuario;
import br.edu.unesc.troiafilmes.exception.AcessoNegadoException;
import br.edu.unesc.troiafilmes.exception.RecursoNaoEncontradoException;
import br.edu.unesc.troiafilmes.exception.RegraDeNegocioException;
import br.edu.unesc.troiafilmes.repository.PerfilRepository;
import br.edu.unesc.troiafilmes.repository.UsuarioRepository;
import br.edu.unesc.troiafilmes.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Cobre as duas regras mais sensíveis do PerfilService: o limite de perfis
 * imposto pelo plano e a checagem de posse.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("PerfilService")
class PerfilServiceTest {

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private AssinaturaService assinaturaService;

    @InjectMocks
    private PerfilService perfilService;

    private static final Long DONO = 1L;
    private static final Long INTRUSO = 99L;

    @Nested
    @DisplayName("limite de perfis do plano")
    class LimiteDoPlano {

        @Test
        @DisplayName("cria o perfil quando ainda há vaga no plano")
        void criaQuandoHaVaga() {
            Usuario dono = usuario(DONO);
            when(assinaturaService.limiteDePerfis(DONO)).thenReturn(5);
            when(perfilRepository.countByUsuarioId(DONO)).thenReturn(2L);
            when(perfilRepository.existsByUsuarioIdAndNomeIgnoreCase(DONO, "Cinema")).thenReturn(false);
            when(usuarioRepository.findById(DONO)).thenReturn(Optional.of(dono));
            when(perfilRepository.save(any(Perfil.class))).thenAnswer(chamada -> {
                Perfil p = chamada.getArgument(0);
                p.setId(10L);
                return p;
            });

            var resposta = perfilService.criar(
                    new PerfilRequest("Cinema", null, false), autenticado(DONO));

            assertThat(resposta.nome()).isEqualTo("Cinema");
            verify(perfilRepository).save(any(Perfil.class));
        }

        @Test
        @DisplayName("bloqueia a criação quando o limite do plano foi atingido")
        void bloqueiaNoLimite() {
            when(assinaturaService.limiteDePerfis(DONO)).thenReturn(1);
            when(perfilRepository.countByUsuarioId(DONO)).thenReturn(1L);
            when(assinaturaService.nomeDoPlanoVigente(DONO)).thenReturn("Básico");

            assertThatThrownBy(() -> perfilService.criar(
                    new PerfilRequest("Segundo", null, false), autenticado(DONO)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("Básico")
                    .hasMessageContaining("no máximo 1");

            verify(perfilRepository, never()).save(any());
        }

        @Test
        @DisplayName("recusa nome repetido na mesma conta")
        void recusaNomeRepetido() {
            when(assinaturaService.limiteDePerfis(DONO)).thenReturn(5);
            when(perfilRepository.countByUsuarioId(DONO)).thenReturn(1L);
            when(perfilRepository.existsByUsuarioIdAndNomeIgnoreCase(DONO, "Principal")).thenReturn(true);

            assertThatThrownBy(() -> perfilService.criar(
                    new PerfilRequest("Principal", null, false), autenticado(DONO)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessageContaining("já possui um perfil");
        }
    }

    @Nested
    @DisplayName("checagem de posse")
    class Posse {

        @Test
        @DisplayName("devolve o perfil para o dono da conta")
        void liberaParaODono() {
            when(perfilRepository.buscarComUsuario(5L))
                    .thenReturn(Optional.of(perfilDe(usuario(DONO))));

            assertThat(perfilService.buscarComPosse(5L, autenticado(DONO)).getId()).isEqualTo(5L);
        }

        @Test
        @DisplayName("nega acesso a perfil de outra conta")
        void negaParaOutraConta() {
            when(perfilRepository.buscarComUsuario(5L))
                    .thenReturn(Optional.of(perfilDe(usuario(DONO))));

            assertThatThrownBy(() -> perfilService.buscarComPosse(5L, autenticado(INTRUSO)))
                    .isInstanceOf(AcessoNegadoException.class)
                    .hasMessageContaining("não pertence ao usuário autenticado");
        }

        @Test
        @DisplayName("nega acesso mesmo quando quem pede é ADMIN")
        void negaTambemParaAdmin() {
            when(perfilRepository.buscarComUsuario(5L))
                    .thenReturn(Optional.of(perfilDe(usuario(DONO))));

            Usuario admin = Usuario.builder().id(INTRUSO).email("admin@troiafilmes.com")
                    .role(Role.ADMIN).ativo(true).build();

            // Perfil, histórico e avaliações são dados pessoais do assinante:
            // ser ADMIN dá acesso ao catálogo, não à conta alheia.
            assertThatThrownBy(() -> perfilService.buscarComPosse(5L, admin))
                    .isInstanceOf(AcessoNegadoException.class);
        }

        @Test
        @DisplayName("informa 404 quando o perfil não existe")
        void perfilInexistente() {
            when(perfilRepository.buscarComUsuario(404L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> perfilService.buscarComPosse(404L, autenticado(DONO)))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }
    }

    @Test
    @DisplayName("impede a exclusão do último perfil da conta")
    void impedeExclusaoDoUltimoPerfil() {
        when(perfilRepository.buscarComUsuario(5L)).thenReturn(Optional.of(perfilDe(usuario(DONO))));
        when(perfilRepository.countByUsuarioId(DONO)).thenReturn(1L);

        assertThatThrownBy(() -> perfilService.remover(5L, autenticado(DONO)))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("ao menos um perfil");

        verify(perfilRepository, never()).delete(any());
    }

    // ------------------------------------------------------------- fixtures

    private static Usuario usuario(Long id) {
        return Usuario.builder()
                .id(id)
                .nome("Usuário " + id)
                .email("usuario%d@troiafilmes.com".formatted(id))
                .role(Role.USER)
                .ativo(true)
                .build();
    }

    private static Perfil perfilDe(Usuario usuario) {
        return Perfil.builder().id(5L).usuario(usuario).nome("Principal").infantil(false).build();
    }

    private static Usuario autenticado(Long id) {
        return usuario(id);
    }
}
