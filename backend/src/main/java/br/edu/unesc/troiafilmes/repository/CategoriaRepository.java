package br.edu.unesc.troiafilmes.repository;

import br.edu.unesc.troiafilmes.entity.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaRepository extends JpaRepository<Categoria, Long> {

    Optional<Categoria> findByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, Long id);

    List<Categoria> findAllByOrderByNomeAsc();

    /** Impede a exclusão de uma categoria que ainda classifica filmes. */
    @Query("SELECT COUNT(f) FROM Filme f JOIN f.categorias c WHERE c.id = :categoriaId")
    long contarFilmesVinculados(@Param("categoriaId") Long categoriaId);
}
