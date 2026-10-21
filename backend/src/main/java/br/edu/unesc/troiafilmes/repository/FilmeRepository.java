package br.edu.unesc.troiafilmes.repository;

import br.edu.unesc.troiafilmes.entity.Filme;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface FilmeRepository extends JpaRepository<Filme, Long>,
        JpaSpecificationExecutor<Filme> {

    boolean existsByTituloIgnoreCaseAndAnoLancamento(String titulo, Integer anoLancamento);

    boolean existsByTituloIgnoreCaseAndAnoLancamentoAndIdNot(String titulo, Integer anoLancamento, Long id);

    /** Carrega um filme já com as categorias, para a tela de detalhe. */
    @Query("SELECT f FROM Filme f LEFT JOIN FETCH f.categorias WHERE f.id = :id")
    Optional<Filme> buscarComCategorias(@Param("id") Long id);

    /**
     * Segunda consulta do padrão "paginar e depois hidratar": a página de
     * filmes é obtida sem join, e as categorias dos ids daquela página são
     * carregadas aqui de uma vez. Evita tanto o N+1 quanto a paginação em
     * memória que o Hibernate faria com um join fetch paginado.
     */
    @Query("SELECT DISTINCT f FROM Filme f LEFT JOIN FETCH f.categorias WHERE f.id IN :ids")
    List<Filme> hidratarCategorias(@Param("ids") Collection<Long> ids);
}
