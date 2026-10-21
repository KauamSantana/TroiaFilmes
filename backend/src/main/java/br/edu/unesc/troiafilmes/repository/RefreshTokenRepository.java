package br.edu.unesc.troiafilmes.repository;

import br.edu.unesc.troiafilmes.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @Query("SELECT rt FROM RefreshToken rt JOIN FETCH rt.usuario WHERE rt.token = :token")
    Optional<RefreshToken> buscarComUsuario(@Param("token") String token);

    /** Revoga todos os tokens do usuário: usado no logout e na troca de senha. */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revogado = true WHERE rt.usuario.id = :usuarioId AND rt.revogado = false")
    int revogarTodosDoUsuario(@Param("usuarioId") Long usuarioId);

    /** Limpeza de tokens que já não servem para nada. */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiraEm < :limite OR rt.revogado = true")
    int removerExpiradosOuRevogados(@Param("limite") LocalDateTime limite);
}
