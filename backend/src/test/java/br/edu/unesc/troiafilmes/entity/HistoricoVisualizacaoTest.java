package br.edu.unesc.troiafilmes.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A regra dos 90% mora na própria entidade, então dá para testá-la sem
 * Spring, banco ou mocks.
 */
@DisplayName("HistoricoVisualizacao")
class HistoricoVisualizacaoTest {

    private static final int DURACAO = 100;

    @ParameterizedTest(name = "{0} de {1} minutos -> concluido={2}")
    @CsvSource({
            "0,   100, false",
            "45,  100, false",
            "89,  100, false",
            "90,  100, true",
            "100, 100, true"
    })
    @DisplayName("marca como concluído a partir de 90% da duração")
    void marcaConclusaoNoLimiteCorreto(int minutos, int duracao, boolean esperado) {
        HistoricoVisualizacao historico = novoHistorico(duracao);

        historico.registrarProgresso(minutos, duracao);

        assertThat(historico.getConcluido()).isEqualTo(esperado);
        assertThat(historico.getMinutosAssistidos()).isEqualTo(minutos);
    }

    @Test
    @DisplayName("calcula o percentual assistido para a barra de progresso")
    void calculaPercentual() {
        HistoricoVisualizacao historico = novoHistorico(DURACAO);

        historico.registrarProgresso(37, DURACAO);

        assertThat(historico.getPercentualAssistido()).isEqualTo(37);
    }

    @Test
    @DisplayName("limita o percentual a 100 mesmo com arredondamento")
    void naoPassaDeCemPorCento() {
        HistoricoVisualizacao historico = novoHistorico(128);

        historico.registrarProgresso(128, 128);

        assertThat(historico.getPercentualAssistido()).isEqualTo(100);
    }

    @Test
    @DisplayName("devolve 0% quando a duração do filme é desconhecida")
    void semDuracaoRetornaZero() {
        HistoricoVisualizacao historico = HistoricoVisualizacao.builder()
                .filme(Filme.builder().id(1L).titulo("Sem duração").build())
                .minutosAssistidos(30)
                .build();

        assertThat(historico.getPercentualAssistido()).isZero();
    }

    @Test
    @DisplayName("reassistir atualiza o registro em vez de zerar o progresso")
    void atualizaProgressoExistente() {
        HistoricoVisualizacao historico = novoHistorico(DURACAO);

        historico.registrarProgresso(20, DURACAO);
        historico.registrarProgresso(95, DURACAO);

        assertThat(historico.getMinutosAssistidos()).isEqualTo(95);
        assertThat(historico.getConcluido()).isTrue();
    }

    private static HistoricoVisualizacao novoHistorico(int duracao) {
        return HistoricoVisualizacao.builder()
                .filme(Filme.builder().id(1L).titulo("Filme de teste")
                        .duracaoMinutos(duracao).build())
                .build();
    }
}
