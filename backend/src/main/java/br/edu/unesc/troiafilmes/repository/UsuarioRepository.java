package br.edu.unesc.troiafilmes.repository;

import br.edu.unesc.troiafilmes.entity.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);

    boolean existsByEmail(String email);

    /** Usado na atualização de cadastro, para não acusar conflito com o próprio registro. */
    boolean existsByEmailAndIdNot(String email, Long id);
}
