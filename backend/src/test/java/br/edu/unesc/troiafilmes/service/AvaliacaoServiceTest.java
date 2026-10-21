package br.edu.unesc.troiafilmes.service;

import br.edu.unesc.troiafilmes.dto.request.AvaliacaoRequest;
import br.edu.unesc.troiafilmes.entity.*;
import br.edu.unesc.troiafilmes.exception.AcessoNegadoException;
import br.edu.unesc.troiafilmes.exception.RegraDeNegocioException;
import br.edu.unesc.troiafilmes.repository.AvaliacaoRepository;
import br.edu.unesc.troiafilmes.entity.Usuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Cobre a regra de avaliação única por perfil/filme e a propagação da
 * checagem de posse feita pelo PerfilService.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AvaliacaoService")
class AvaliacaoServiceTest {

    @Mock
    private AvaliacaoRepository avaliacaoRepository;

    @Mock
    private PerfilService perfilService;

    @Mock
    private FilmeService filmeService;

    @InjectMocks
    private AvaliacaoService avaliacaoService;

    private static final Long PERFIL_ID = 5L;
    private static final Long FILME_ID = 1L;

    @Test
    @DisplayName("registra a avaliação na primeira vez")
    void registraPrimeiraAvaliacao() {
        when(perfilService.buscarComPosse(eq(PERFIL_ID), any())).thenReturn(perfil());
        when(filmeService.buscarEntidade(FILME_ID)).thenReturn(filme());
        when(avaliacaoRepository.existsByPerfilIdAndFilmeId(PERFIL_ID, FILME_ID)).thenReturn(false);
        when(avaliacaoRepository.save(any(Avaliacao.class))).thenAnswer(chamada -> {
            Avaliacao a = chamada.getArgument(0);
            a.setId(100L);
            return a;
        });

        var resposta = avaliacaoService.criar(
                PERFIL_ID, new AvaliacaoRequest(FILME_ID, 5, "Excelente."), autenticado());

        assertThat(resposta.nota()).isEqualTo(5);
        assertThat(resposta.filmeTitulo()).isEqualTo("Interestelar");
        verify(avaliacaoRepository).save(any(Avaliacao.class));
    }

    @Test
    @DisplayName("recusa uma segunda avaliação do mesmo perfil para o mesmo filme")
    void recusaAvaliacaoDuplicada() {
        when(perfilService.buscarComPosse(eq(PERFIL_ID), any())).thenReturn(perfil());
        when(filmeService.buscarEntidade(FILME_ID)).thenReturn(filme());
        when(avaliacaoRepository.existsByPerfilIdAndFilmeId(PERFIL_ID, FILME_ID)).thenReturn(true);

        assertThatThrownBy(() -> avaliacaoService.criar(
                PERFIL_ID, new AvaliacaoRequest(FILME_ID, 4, "De novo."), autenticado()))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("já avaliou o filme")
                .hasMessageContaining("Edite a avaliação existente");

        verify(avaliacaoRepository, never()).save(any());
    }

    @Test
    @DisplayName("não avalia usando perfil de outra conta")
    void naoAvaliaComPerfilAlheio() {
        when(perfilService.buscarComPosse(eq(PERFIL_ID), any()))
                .thenThrow(AcessoNegadoException.doRecurso("Este perfil"));

        assertThatThrownBy(() -> avaliacaoService.criar(
                PERFIL_ID, new AvaliacaoRequest(FILME_ID, 5, null), autenticado()))
                .isInstanceOf(AcessoNegadoException.class);

        // Nem chega a consultar o filme: a posse é verificada antes de tudo.
        verifyNoInteractions(filmeService);
        verify(avaliacaoRepository, never()).save(any());
    }

    // ------------------------------------------------------------- fixtures

    private static Usuario usuario() {
        return Usuario.builder().id(1L).nome("Usuário Demonstração")
                .email("usuario@troiafilmes.com").role(Role.USER).ativo(true).build();
    }

    private static Perfil perfil() {
        return Perfil.builder().id(PERFIL_ID).usuario(usuario()).nome("Principal").build();
    }

    private static Filme filme() {
        return Filme.builder().id(FILME_ID).titulo("Interestelar")
                .anoLancamento(2023).duracaoMinutos(128).build();
    }

    private static Usuario autenticado() {
        return usuario();
    }
}
