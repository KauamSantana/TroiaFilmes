package br.edu.unesc.troiafilmes.controller;

import br.edu.unesc.troiafilmes.dto.request.PlanoRequest;
import br.edu.unesc.troiafilmes.dto.response.PlanoResponse;
import br.edu.unesc.troiafilmes.service.PlanoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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

/** Planos de assinatura. Leitura para autenticados, escrita só para ADMIN. */
@RestController
@RequestMapping("/api/planos")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "7. Planos", description = "Planos de assinatura do TroiaFilmes")
public class PlanoController {

    private final PlanoService planoService;

    @GetMapping
    @Operation(summary = "Lista os planos, do mais barato ao mais caro")
    public List<PlanoResponse> listar(
            @Parameter(description = "Quando true, omite os planos desativados", example = "true")
            @RequestParam(defaultValue = "true") boolean apenasAtivos) {
        return planoService.listar(apenasAtivos);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um plano")
    @ApiResponse(responseCode = "404", description = "Plano inexistente")
    public PlanoResponse buscarPorId(@PathVariable Long id) {
        return planoService.buscarPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cadastra um plano")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Plano criado"),
            @ApiResponse(responseCode = "409", description = "Já existe plano com esse nome")
    })
    public PlanoResponse criar(@Valid @RequestBody PlanoRequest request) {
        return planoService.criar(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza um plano")
    public PlanoResponse atualizar(@PathVariable Long id,
                                   @Valid @RequestBody PlanoRequest request) {
        return planoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
            summary = "Remove um plano",
            description = "Planos com assinaturas ativas não podem ser excluídos; desative-os.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Plano removido"),
            @ApiResponse(responseCode = "409", description = "O plano possui assinantes ativos")
    })
    public void remover(@PathVariable Long id) {
        planoService.remover(id);
    }
}
