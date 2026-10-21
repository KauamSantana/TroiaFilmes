package br.edu.unesc.troiafilmes.controller;

import br.edu.unesc.troiafilmes.dto.response.ItemListaResponse;
import br.edu.unesc.troiafilmes.entity.Usuario;
import br.edu.unesc.troiafilmes.service.MinhaListaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** Minha Lista: filmes salvos por um perfil para assistir depois. */
@RestController
@RequestMapping("/api/perfis/{perfilId}/lista")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "10. Minha Lista", description = "Filmes salvos por um perfil")
public class MinhaListaController {

    private final MinhaListaService minhaListaService;

    @GetMapping
    @Operation(summary = "Lista os filmes salvos, do mais recente para o mais antigo")
    @ApiResponse(responseCode = "403", description = "O perfil pertence a outra conta")
    public List<ItemListaResponse> listar(@PathVariable Long perfilId,
                                          @AuthenticationPrincipal Usuario autenticado) {
        return minhaListaService.listar(perfilId, autenticado);
    }

    @PostMapping("/{filmeId}")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Adiciona um filme à lista do perfil")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Filme adicionado"),
            @ApiResponse(responseCode = "404", description = "Perfil ou filme inexistente"),
            @ApiResponse(responseCode = "409", description = "O filme já está na lista")
    })
    public ItemListaResponse adicionar(@PathVariable Long perfilId,
                                       @PathVariable Long filmeId,
                                       @AuthenticationPrincipal Usuario autenticado) {
        return minhaListaService.adicionar(perfilId, filmeId, autenticado);
    }

    @DeleteMapping("/{filmeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um filme da lista do perfil")
    @ApiResponse(responseCode = "204", description = "Filme removido da lista")
    public void remover(@PathVariable Long perfilId,
                        @PathVariable Long filmeId,
                        @AuthenticationPrincipal Usuario autenticado) {
        minhaListaService.remover(perfilId, filmeId, autenticado);
    }

    @GetMapping("/{filmeId}")
    @Operation(
            summary = "Informa se um filme está na lista",
            description = "Usado pela tela de detalhe para decidir entre os botões "
                    + "Adicionar e Remover.")
    public Map<String, Boolean> contem(@PathVariable Long perfilId,
                                       @PathVariable Long filmeId,
                                       @AuthenticationPrincipal Usuario autenticado) {
        return Map.of("naLista", minhaListaService.contem(perfilId, filmeId, autenticado));
    }
}
