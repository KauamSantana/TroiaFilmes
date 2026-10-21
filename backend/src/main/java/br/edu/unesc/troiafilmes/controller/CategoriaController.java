package br.edu.unesc.troiafilmes.controller;

import br.edu.unesc.troiafilmes.dto.request.CategoriaRequest;
import br.edu.unesc.troiafilmes.dto.response.CategoriaResponse;
import br.edu.unesc.troiafilmes.service.CategoriaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Categorias do catálogo. Leitura para autenticados, escrita só para ADMIN. */
@RestController
@RequestMapping("/api/categorias")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "5. Categorias", description = "Gêneros usados para classificar os filmes")
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    @Operation(summary = "Lista todas as categorias em ordem alfabética")
    public List<CategoriaResponse> listar() {
        return categoriaService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma categoria")
    @ApiResponse(responseCode = "404", description = "Categoria inexistente")
    public CategoriaResponse buscarPorId(@PathVariable Long id) {
        return categoriaService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cadastra uma categoria")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Categoria criada"),
            @ApiResponse(responseCode = "403", description = "Requer papel ADMIN"),
            @ApiResponse(responseCode = "409", description = "Já existe categoria com esse nome")
    })
    public CategoriaResponse criar(@Valid @RequestBody CategoriaRequest request) {
        return categoriaService.criar(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza uma categoria")
    public CategoriaResponse atualizar(@PathVariable Long id,
                                       @Valid @RequestBody CategoriaRequest request) {
        return categoriaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Remove uma categoria",
            description = "Só é possível excluir categorias que não classificam nenhum filme.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Categoria removida"),
            @ApiResponse(responseCode = "409", description = "A categoria ainda classifica filmes")
    })
    public void remover(@PathVariable Long id) {
        categoriaService.remover(id);
    }
}
