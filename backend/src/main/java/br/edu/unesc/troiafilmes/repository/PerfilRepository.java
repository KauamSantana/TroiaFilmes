package br.edu.unesc.troiafilmes.repository;

import br.edu.unesc.troiafilmes.entity.Perfil;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PerfilRepository extends JpaRepository<Perfil, Long> {

    List<Perfil> findByUsuarioIdOrderByCriadoEmAsc(Long usuarioId);

    /** Base da regra de limite de perfis por plano. */
    long countByUsuarioId(Long usuarioId);

    boolean existsByUsuarioIdAndNomeIgnoreCase(Long usuarioId, String nome);

    boolean existsByUsuarioIdAndNomeIgnoreCaseAndIdNot(Long usuarioId, String nome, Long id);

    /** Carrega o perfil junto do dono, evitando consulta extra na checagem de posse. */
    @Query("SELECT p FROM Perfil p JOIN FETCH p.usuario WHERE p.id = :id")
    Optional<Perfil> buscarComUsuario(@Param("id") Long id);
}
