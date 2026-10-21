package br.edu.unesc.troiafilmes.controller;

import br.edu.unesc.troiafilmes.dto.response.PaginaResponse;
import br.edu.unesc.troiafilmes.dto.response.UsuarioResponse;
import br.edu.unesc.troiafilmes.entity.Role;
import br.edu.unesc.troiafilmes.entity.Usuario;
import br.edu.unesc.troiafilmes.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Administração de contas. Todas as rotas exigem papel ADMIN, garantido em
 * duas frentes: pela regra de rota no {@code SecurityConfig} e pelo
 * {@code @PreAuthorize} declarado aqui.
 */
@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "2. Usuários (ADMIN)", description = "Administração das contas do TroiaFilmes")
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    @Operation(summary = "Lista as contas cadastradas, de forma paginada")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de usuários"),
            @ApiResponse(responseCode = "403", description = "Requer papel ADMIN")
    })
    public PaginaResponse<UsuarioResponse> listar(
            @PageableDefault(size = 20, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return usuarioService.listar(pageable);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Detalha uma conta")
    @ApiResponse(responseCode = "404", description = "Usuário inexistente")
    public UsuarioResponse buscarPorId(@PathVariable Long id) {
        return usuarioService.buscarPorId(id);
    }

    @PatchMapping("/{id}/situacao")
    @Operation(
            summary = "Ativa ou desativa uma conta",
            description = "Desativar é um soft delete: o registro permanece no banco, mas a "
                    + "conta deixa de autenticar e tem as sessões encerradas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Situação alterada"),
            @ApiResponse(responseCode = "409", description = "Um administrador não pode desativar a própria conta")
    })
    public UsuarioResponse alterarSituacao(
            @PathVariable Long id,
            @Parameter(description = "true reativa, false desativa", example = "false")
            @RequestParam boolean ativo,
            @AuthenticationPrincipal Usuario autenticado) {
        return usuarioService.alterarSituacao(id, ativo, autenticado.getId());
    }

    @PatchMapping("/{id}/papel")
    @Operation(
            summary = "Promove ou rebaixa o papel de uma conta",
            description = "Como o papel viaja dentro do JWT, a alteração revoga as sessões "
                    + "abertas da conta afetada.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Papel alterado"),
            @ApiResponse(responseCode = "409", description = "Um administrador não pode rebaixar a si mesmo")
    })
    public UsuarioResponse alterarPapel(
            @PathVariable Long id,
            @Parameter(description = "Novo papel", example = "ADMIN") @RequestParam Role role,
            @AuthenticationPrincipal Usuario autenticado) {
        return usuarioService.alterarPapel(id, role, autenticado.getId());
    }
}
