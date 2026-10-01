package br.edu.unesc.troiafilmes.repository;

import br.edu.unesc.troiafilmes.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    /** Usado no login e na validação do token: o e-mail é o identificador da conta. */
    Optional<Usuario> findByEmail(String email);

    /** Usado no cadastro, para recusar e-mail repetido antes de bater na constraint. */
    boolean existsByEmail(String email);
}
