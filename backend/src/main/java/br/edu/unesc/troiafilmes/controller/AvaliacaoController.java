package br.edu.unesc.troiafilmes.controller;

import br.edu.unesc.troiafilmes.dto.request.AvaliacaoRequest;
import br.edu.unesc.troiafilmes.dto.response.AvaliacaoResponse;
import br.edu.unesc.troiafilmes.entity.Usuario;
import br.edu.unesc.troiafilmes.service.AvaliacaoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Avaliações de filmes.
 *
 * <p>A criação fica sob o perfil que avalia ({@code /api/perfis/{id}/avaliacoes}),
 * porque é o perfil, e não a conta, que dá a nota. Edição e exclusão usam o id
 * da própria avaliação.</p>
 */
@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "9. Avaliações", description = "Notas e comentários dos perfis sobre os filmes")
public class AvaliacaoController {

    private final AvaliacaoService avaliacaoService;

    @GetMapping("/api/perfis/{perfilId}/avaliacoes")
    @Operation(summary = "Lista as avaliações feitas por um perfil")
    @ApiResponse(responseCode = "403", description = "O perfil pertence a outra conta")
    public List<AvaliacaoResponse> listarDoPerfil(
            @PathVariable Long perfilId,
            @AuthenticationPrincipal Usuario autenticado) {
        return avaliacaoService.listarDoPerfil(perfilId, autenticado);
    }

    @PostMapping("/api/perfis/{perfilId}/avaliacoes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Avalia um filme",
            description = "Cada perfil avalia um mesmo filme uma única vez.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Avaliação registrada"),
            @ApiResponse(responseCode = "400", description = "Nota fora do intervalo de 1 a 5"),
            @ApiResponse(responseCode = "403", description = "O perfil pertence a outra conta"),
            @ApiResponse(responseCode = "404", description = "Perfil ou filme inexistente"),
            @ApiResponse(responseCode = "409", description = "O perfil já avaliou este filme")
    })
    public AvaliacaoResponse criar(@PathVariable Long perfilId,
                                   @Valid @RequestBody AvaliacaoRequest request,
                                   @AuthenticationPrincipal Usuario autenticado) {
        return avaliacaoService.criar(perfilId, request, autenticado);
    }

    @PutMapping("/api/avaliacoes/{id}")
    @Operation(
            summary = "Altera a nota e o comentário de uma avaliação",
            description = "O filme avaliado não muda.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Avaliação atualizada"),
            @ApiResponse(responseCode = "403", description = "A avaliação é de outra conta")
    })
    public AvaliacaoResponse atualizar(@PathVariable Long id,
                                       @Valid @RequestBody AvaliacaoRequest request,
                                       @AuthenticationPrincipal Usuario autenticado) {
        return avaliacaoService.atualizar(id, request, autenticado);
    }

    @DeleteMapping("/api/avaliacoes/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove uma avaliação")
    @ApiResponse(responseCode = "204", description = "Avaliação removida")
    public void remover(@PathVariable Long id,
                        @AuthenticationPrincipal Usuario autenticado) {
        avaliacaoService.remover(id, autenticado);
    }
}
