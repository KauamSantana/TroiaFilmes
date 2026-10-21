package br.edu.unesc.troiafilmes.repository;

import br.edu.unesc.troiafilmes.entity.Avaliacao;
import br.edu.unesc.troiafilmes.repository.projection.ResumoAvaliacaoProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface AvaliacaoRepository extends JpaRepository<Avaliacao, Long> {

    /** Sustenta a regra "um perfil avalia cada filme uma única vez". */
    boolean existsByPerfilIdAndFilmeId(Long perfilId, Long filmeId);

    Optional<Avaliacao> findByPerfilIdAndFilmeId(Long perfilId, Long filmeId);

    List<Avaliacao> findByPerfilIdOrderByCriadoEmDesc(Long perfilId);

    Page<Avaliacao> findByFilmeIdOrderByCriadoEmDesc(Long filmeId, Pageable pageable);

    @Query("SELECT a FROM Avaliacao a JOIN FETCH a.perfil JOIN FETCH a.filme WHERE a.id = :id")
    Optional<Avaliacao> buscarComRelacionamentos(@Param("id") Long id);

    /**
     * Nota média e total de avaliações por filme, em uma única consulta.
     * Para um filme só, basta passar uma lista de um elemento.
     */
    @Query("""
            SELECT a.filme.id AS filmeId,
                   AVG(a.nota) AS notaMedia,
                   COUNT(a)    AS totalAvaliacoes
              FROM Avaliacao a
             WHERE a.filme.id IN :filmeIds
             GROUP BY a.filme.id
            """)
    List<ResumoAvaliacaoProjection> resumirPorFilmes(@Param("filmeIds") Collection<Long> filmeIds);
}
