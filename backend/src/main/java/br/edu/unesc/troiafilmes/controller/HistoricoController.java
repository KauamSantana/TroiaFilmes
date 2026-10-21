package br.edu.unesc.troiafilmes.controller;

import br.edu.unesc.troiafilmes.dto.request.ProgressoRequest;
import br.edu.unesc.troiafilmes.dto.response.HistoricoResponse;
import br.edu.unesc.troiafilmes.entity.Usuario;
import br.edu.unesc.troiafilmes.service.HistoricoService;
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

/** Histórico de visualização e prateleira "Continuar assistindo". */
@RestController
@RequestMapping("/api/perfis/{perfilId}")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "11. Histórico", description = "Progresso de visualização por perfil")
public class HistoricoController {

    private final HistoricoService historicoService;

    @GetMapping("/historico")
    @Operation(summary = "Histórico completo do perfil")
    @ApiResponse(responseCode = "403", description = "O perfil pertence a outra conta")
    public List<HistoricoResponse> listar(@PathVariable Long perfilId,
                                          @AuthenticationPrincipal Usuario autenticado) {
        return historicoService.listar(perfilId, autenticado);
    }

    @GetMapping("/continuar-assistindo")
    @Operation(
            summary = "Filmes começados e ainda não concluídos",
            description = "Cada item traz o percentual assistido, para a barra de progresso.")
    public List<HistoricoResponse> continuarAssistindo(
            @PathVariable Long perfilId,
            @AuthenticationPrincipal Usuario autenticado) {
        return historicoService.continuarAssistindo(perfilId, autenticado);
    }

    @PostMapping("/historico")
    @Operation(
            summary = "Registra o progresso de reprodução",
            description = "Cria o registro na primeira vez e atualiza nas seguintes. "
                    + "O filme é marcado como concluído ao atingir 90% da duração.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Progresso registrado"),
            @ApiResponse(responseCode = "404", description = "Perfil ou filme inexistente"),
            @ApiResponse(responseCode = "409", description = "Minutos informados excedem a duração do filme")
    })
    public HistoricoResponse registrarProgresso(
            @PathVariable Long perfilId,
            @Valid @RequestBody ProgressoRequest request,
            @AuthenticationPrincipal Usuario autenticado) {
        return historicoService.registrarProgresso(perfilId, request, autenticado);
    }

    @DeleteMapping("/historico/{filmeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove um filme do histórico do perfil")
    @ApiResponse(responseCode = "204", description = "Registro removido")
    public void remover(@PathVariable Long perfilId,
                        @PathVariable Long filmeId,
                        @AuthenticationPrincipal Usuario autenticado) {
        historicoService.remover(perfilId, filmeId, autenticado);
    }
}
