package br.edu.unesc.troiafilmes.repository.specification;

import br.edu.unesc.troiafilmes.entity.Categoria;
import br.edu.unesc.troiafilmes.entity.Filme;
import jakarta.persistence.criteria.Join;
import org.springframework.data.jpa.domain.Specification;

/**
 * Filtros compostos da busca no catálogo.
 *
 * <p>Usar Specification em vez de uma JPQL com {@code :param IS NULL} evita
 * que o PostgreSQL receba parâmetros sem tipo definido quando o filtro não é
 * informado: aqui, o predicado simplesmente não entra na consulta.</p>
 */
public final class FilmeSpecification {

    private FilmeSpecification() {
        // classe utilitária
    }

    /** Busca por parte do título, sem diferenciar maiúsculas de minúsculas. */
    public static Specification<Filme> tituloContem(String titulo) {
        if (titulo == null || titulo.isBlank()) {
            return null;
        }
        String padrao = "%" + titulo.trim().toLowerCase() + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("titulo")), padrao);
    }

    public static Specification<Filme> daCategoria(Long categoriaId) {
        if (categoriaId == null) {
            return null;
        }
        return (root, query, cb) -> {
            Join<Filme, Categoria> categorias = root.join("categorias");
            if (query != null) {
                query.distinct(true);
            }
            return cb.equal(categorias.get("id"), categoriaId);
        };
    }

    public static Specification<Filme> doAno(Integer ano) {
        if (ano == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("anoLancamento"), ano);
    }

    /** Combina os filtros informados, ignorando os nulos. */
    public static Specification<Filme> comFiltros(String titulo, Long categoriaId, Integer ano) {
        return Specification.allOf(tituloContem(titulo), daCategoria(categoriaId), doAno(ano));
    }
}
