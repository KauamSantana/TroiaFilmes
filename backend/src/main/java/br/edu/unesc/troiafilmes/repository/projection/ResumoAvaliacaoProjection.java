package br.edu.unesc.troiafilmes.repository.projection;

/**
 * Projeção usada para trazer nota média e total de avaliações de vários
 * filmes em uma única consulta, evitando o problema de N+1 na listagem do
 * catálogo.
 */
public interface ResumoAvaliacaoProjection {

    Long getFilmeId();

    Double getNotaMedia();

    Long getTotalAvaliacoes();
}
