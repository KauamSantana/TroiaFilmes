package br.edu.unesc.troiafilmes.repository;

import br.edu.unesc.troiafilmes.entity.HistoricoVisualizacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HistoricoVisualizacaoRepository extends JpaRepository<HistoricoVisualizacao, Long> {

    Optional<HistoricoVisualizacao> findByPerfilIdAndFilmeId(Long perfilId, Long filmeId);

    /** Histórico completo do perfil, do mais recente para o mais antigo. */
    @Query("""
            SELECT h FROM HistoricoVisualizacao h
              JOIN FETCH h.filme
             WHERE h.perfil.id = :perfilId
             ORDER BY h.assistidoEm DESC
            """)
    List<HistoricoVisualizacao> listarPorPerfil(@Param("perfilId") Long perfilId);

    /** Alimenta a prateleira "Continuar assistindo": o que foi começado e não terminado. */
    @Query("""
            SELECT h FROM HistoricoVisualizacao h
              JOIN FETCH h.filme
             WHERE h.perfil.id = :perfilId
               AND h.concluido = false
               AND h.minutosAssistidos > 0
             ORDER BY h.assistidoEm DESC
            """)
    List<HistoricoVisualizacao> listarEmAndamento(@Param("perfilId") Long perfilId);
}
