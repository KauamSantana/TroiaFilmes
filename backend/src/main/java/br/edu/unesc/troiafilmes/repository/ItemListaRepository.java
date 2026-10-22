package br.edu.unesc.troiafilmes.repository;

import br.edu.unesc.troiafilmes.entity.ItemLista;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ItemListaRepository extends JpaRepository<ItemLista, Long> {

    boolean existsByPerfilIdAndFilmeId(Long perfilId, Long filmeId);

    Optional<ItemLista> findByPerfilIdAndFilmeId(Long perfilId, Long filmeId);

    /** Minha Lista do perfil, do item mais recente para o mais antigo. */
    @Query("""
            SELECT il FROM ItemLista il
              JOIN FETCH il.filme f
             WHERE il.perfil.id = :perfilId
             ORDER BY il.adicionadoEm DESC
            """)
    List<ItemLista> listarPorPerfil(@Param("perfilId") Long perfilId);
}
