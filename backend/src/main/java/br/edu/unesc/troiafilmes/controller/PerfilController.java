package br.edu.unesc.troiafilmes.controller;

import br.edu.unesc.troiafilmes.dto.request.PerfilRequest;
import br.edu.unesc.troiafilmes.dto.response.PerfilResponse;
import br.edu.unesc.troiafilmes.entity.Usuario;
import br.edu.unesc.troiafilmes.service.PerfilService;
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
 * Perfis de consumo da conta autenticada.
 *
 * <p>Nenhuma rota aceita um id de usuário: a conta vem sempre do token, e a
 * posse de cada perfil é conferida no service.</p>
 */
@RestController
@RequestMapping("/api/perfis")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "6. Perfis", description = "Perfis de consumo da conta autenticada")
public class PerfilController {

    private final PerfilService perfilService;

    @GetMapping
    @Operation(summary = "Lista os perfis da conta autenticada")
    public List<PerfilResponse> listar(@AuthenticationPrincipal Usuario autenticado) {
        return perfilService.listarDoUsuario(autenticado.getId());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha um perfil da conta")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Perfil encontrado"),
            @ApiResponse(responseCode = "403", description = "O perfil pertence a outra conta"),
            @ApiResponse(responseCode = "404", description = "Perfil inexistente")
    })
    public PerfilResponse buscarPorId(@PathVariable Long id,
                                      @AuthenticationPrincipal Usuario autenticado) {
        return perfilService.buscarPorId(id, autenticado);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cria um perfil",
            description = "A quantidade de perfis é limitada pelo plano da assinatura ativa.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Perfil criado"),
            @ApiResponse(responseCode = "409", description = "Limite do plano atingido ou nome repetido")
    })
    public PerfilResponse criar(@Valid @RequestBody PerfilRequest request,
                                @AuthenticationPrincipal Usuario autenticado) {
        return perfilService.criar(request, autenticado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza um perfil")
    public PerfilResponse atualizar(@PathVariable Long id,
                                    @Valid @RequestBody PerfilRequest request,
                                    @AuthenticationPrincipal Usuario autenticado) {
        return perfilService.atualizar(id, request, autenticado);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Remove um perfil",
            description = "Remove junto as avaliações, a Minha Lista e o histórico do perfil. "
                    + "A conta precisa manter ao menos um perfil.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Perfil removido"),
            @ApiResponse(responseCode = "409", description = "É o último perfil da conta")
    })
    public void remover(@PathVariable Long id,
                        @AuthenticationPrincipal Usuario autenticado) {
        perfilService.remover(id, autenticado);
    }
}
