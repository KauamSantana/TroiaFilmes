package br.edu.unesc.troiafilmes.repository;

import br.edu.unesc.troiafilmes.entity.Assinatura;
import br.edu.unesc.troiafilmes.entity.StatusAssinatura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssinaturaRepository extends JpaRepository<Assinatura, Long> {

    /**
     * Assinatura vigente do usuário, já com o plano carregado: é dela que sai
     * o limite de perfis aplicado na criação de novos perfis.
     */
    @Query("""
            SELECT a FROM Assinatura a
              JOIN FETCH a.plano
             WHERE a.usuario.id = :usuarioId
               AND a.status = br.edu.unesc.troiafilmes.entity.StatusAssinatura.ATIVA
            """)
    Optional<Assinatura> buscarAtivaComPlano(@Param("usuarioId") Long usuarioId);

    @Query("""
            SELECT a FROM Assinatura a
              JOIN FETCH a.plano
             WHERE a.usuario.id = :usuarioId
             ORDER BY a.dataInicio DESC, a.id DESC
            """)
    List<Assinatura> listarHistorico(@Param("usuarioId") Long usuarioId);

    long countByPlanoIdAndStatus(Long planoId, StatusAssinatura status);
}
