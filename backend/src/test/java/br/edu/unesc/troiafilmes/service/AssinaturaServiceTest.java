package br.edu.unesc.troiafilmes.service;

import br.edu.unesc.troiafilmes.entity.*;
import br.edu.unesc.troiafilmes.exception.RegraDeNegocioException;
import br.edu.unesc.troiafilmes.repository.AssinaturaRepository;
import br.edu.unesc.troiafilmes.repository.PerfilRepository;
import br.edu.unesc.troiafilmes.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Cobre a regra de uma única assinatura ativa por conta e a validação de
 * capacidade ao trocar de plano.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AssinaturaService")
class AssinaturaServiceTest {

    @Mock
    private AssinaturaRepository assinaturaRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PerfilRepository perfilRepository;

    @Mock
    private PlanoService planoService;

    @InjectMocks
    private AssinaturaService assinaturaService;

    private static final Long USUARIO_ID = 1L;

    @Test
    @DisplayName("cancela a assinatura anterior ao contratar outro plano")
    void cancelaAnteriorAoTrocarDePlano() {
        Plano basico = plano(1L, "Básico", 1);
        Plano premium = plano(3L, "Premium", 5);
        Assinatura vigente = assinatura(basico);

        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(planoService.buscarEntidade(3L)).thenReturn(premium);
        when(assinaturaRepository.buscarAtivaComPlano(USUARIO_ID)).thenReturn(Optional.of(vigente));
        when(perfilRepository.countByUsuarioId(USUARIO_ID)).thenReturn(1L);
        when(assinaturaRepository.saveAndFlush(any(Assinatura.class))).thenAnswer(c -> c.getArgument(0));
        when(assinaturaRepository.save(any(Assinatura.class))).thenAnswer(c -> c.getArgument(0));

        var resposta = assinaturaService.contratar(USUARIO_ID, 3L);

        assertThat(resposta.plano().nome()).isEqualTo("Premium");
        assertThat(resposta.status()).isEqualTo(StatusAssinatura.ATIVA);

        // A anterior precisa ter sido encerrada: só pode existir uma ATIVA.
        assertThat(vigente.getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
        assertThat(vigente.getDataFim()).isNotNull();

        // A ordem importa. Como o id é IDENTITY, o save() da nova assinatura
        // dispara o INSERT imediatamente; se o cancelamento da anterior não
        // tiver sido descarregado antes, o índice único parcial
        // uk_assinatura_ativa_por_usuario rejeita a operação no banco.
        ArgumentCaptor<Assinatura> cancelada = ArgumentCaptor.forClass(Assinatura.class);
        ArgumentCaptor<Assinatura> criada = ArgumentCaptor.forClass(Assinatura.class);

        InOrder ordem = inOrder(assinaturaRepository);
        ordem.verify(assinaturaRepository).saveAndFlush(cancelada.capture());
        ordem.verify(assinaturaRepository).save(criada.capture());

        assertThat(cancelada.getValue().getStatus()).isEqualTo(StatusAssinatura.CANCELADA);
        assertThat(criada.getValue().getStatus()).isEqualTo(StatusAssinatura.ATIVA);
    }

    @Test
    @DisplayName("recusa migração para plano que comporta menos perfis do que a conta usa")
    void recusaDowngradeQueDeixariaPerfisOrfaos() {
        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(planoService.buscarEntidade(1L)).thenReturn(plano(1L, "Básico", 1));
        when(assinaturaRepository.buscarAtivaComPlano(USUARIO_ID))
                .thenReturn(Optional.of(assinatura(plano(3L, "Premium", 5))));
        when(perfilRepository.countByUsuarioId(USUARIO_ID)).thenReturn(4L);

        assertThatThrownBy(() -> assinaturaService.contratar(USUARIO_ID, 1L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Você possui 4 perfis")
                .hasMessageContaining("Exclua 3 perfil(is)");

        verify(assinaturaRepository, never()).save(any());
    }

    @Test
    @DisplayName("recusa contratar o plano que já está vigente")
    void recusaContratarOMesmoPlano() {
        Plano premium = plano(3L, "Premium", 5);

        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(planoService.buscarEntidade(3L)).thenReturn(premium);
        when(assinaturaRepository.buscarAtivaComPlano(USUARIO_ID))
                .thenReturn(Optional.of(assinatura(premium)));

        assertThatThrownBy(() -> assinaturaService.contratar(USUARIO_ID, 3L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já é a sua assinatura atual");
    }

    @Test
    @DisplayName("recusa contratar plano desativado")
    void recusaPlanoDesativado() {
        Plano desativado = plano(9L, "Legado", 2);
        desativado.setAtivo(false);

        when(usuarioRepository.findById(USUARIO_ID)).thenReturn(Optional.of(usuario()));
        when(planoService.buscarEntidade(9L)).thenReturn(desativado);

        assertThatThrownBy(() -> assinaturaService.contratar(USUARIO_ID, 9L))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("não está disponível para contratação");
    }

    @Test
    @DisplayName("sem assinatura ativa, a conta fica limitada a um perfil")
    void limitePadraoSemAssinatura() {
        when(assinaturaRepository.buscarAtivaComPlano(USUARIO_ID)).thenReturn(Optional.empty());

        assertThat(assinaturaService.limiteDePerfis(USUARIO_ID)).isEqualTo(1);
    }

    @Test
    @DisplayName("o limite de perfis vem do plano vigente")
    void limiteVemDoPlanoVigente() {
        when(assinaturaRepository.buscarAtivaComPlano(USUARIO_ID))
                .thenReturn(Optional.of(assinatura(plano(3L, "Premium", 5))));

        assertThat(assinaturaService.limiteDePerfis(USUARIO_ID)).isEqualTo(5);
        assertThat(assinaturaService.nomeDoPlanoVigente(USUARIO_ID)).isEqualTo("Premium");
    }

    // ------------------------------------------------------------- fixtures

    private static Usuario usuario() {
        return Usuario.builder().id(USUARIO_ID).nome("Usuário Demonstração")
                .email("usuario@troiafilmes.com").role(Role.USER).ativo(true).build();
    }

    private static Plano plano(Long id, String nome, int maxPerfis) {
        return Plano.builder()
                .id(id).nome(nome).preco(new BigDecimal("34.90"))
                .maxPerfis(maxPerfis).qualidadeMaxima(Qualidade.HD).ativo(true)
                .build();
    }

    private static Assinatura assinatura(Plano plano) {
        return Assinatura.builder()
                .id(50L).usuario(usuario()).plano(plano)
                .status(StatusAssinatura.ATIVA)
                .build();
    }
}
