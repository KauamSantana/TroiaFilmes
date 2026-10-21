package br.edu.unesc.troiafilmes.repository;

import br.edu.unesc.troiafilmes.entity.Plano;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlanoRepository extends JpaRepository<Plano, Long> {

    Optional<Plano> findByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    List<Plano> findByAtivoTrueOrderByPrecoAsc();

    List<Plano> findAllByOrderByPrecoAsc();
}
