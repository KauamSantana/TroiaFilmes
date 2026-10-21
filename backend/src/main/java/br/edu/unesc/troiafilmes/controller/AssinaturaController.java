package br.edu.unesc.troiafilmes.controller;

import br.edu.unesc.troiafilmes.dto.request.AssinaturaRequest;
import br.edu.unesc.troiafilmes.dto.response.AssinaturaResponse;
import br.edu.unesc.troiafilmes.entity.Usuario;
import br.edu.unesc.troiafilmes.service.AssinaturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Assinatura da conta autenticada. */
@RestController
@RequestMapping("/api/assinaturas")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "8. Assinatura", description = "Contratação e cancelamento de planos")
public class AssinaturaController {

    private final AssinaturaService assinaturaService;

    @GetMapping("/ativa")
    @Operation(summary = "Assinatura vigente da conta autenticada")
    @ApiResponse(responseCode = "404", description = "A conta não possui assinatura ativa")
    public AssinaturaResponse buscarAtiva(@AuthenticationPrincipal Usuario autenticado) {
        return assinaturaService.buscarAtiva(autenticado.getId());
    }

    @GetMapping("/historico")
    @Operation(summary = "Histórico de assinaturas da conta, da mais recente para a mais antiga")
    public List<AssinaturaResponse> listarHistorico(
            @AuthenticationPrincipal Usuario autenticado) {
        return assinaturaService.listarHistorico(autenticado.getId());
    }

    @PostMapping
    @Operation(
            summary = "Contrata um plano",
            description = "Cancela automaticamente a assinatura anterior, já que só pode "
                    + "existir uma ativa por conta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Plano contratado"),
            @ApiResponse(responseCode = "404", description = "Plano inexistente"),
            @ApiResponse(responseCode = "409",
                    description = "Plano desativado, já vigente, ou com menos perfis do que a conta usa")
    })
    public AssinaturaResponse contratar(@Valid @RequestBody AssinaturaRequest request,
                                        @AuthenticationPrincipal Usuario autenticado) {
        return assinaturaService.contratar(autenticado.getId(), request.planoId());
    }

    @DeleteMapping
    @Operation(summary = "Cancela a assinatura vigente")
    @ApiResponse(responseCode = "404", description = "A conta não possui assinatura ativa")
    public AssinaturaResponse cancelar(@AuthenticationPrincipal Usuario autenticado) {
        return assinaturaService.cancelar(autenticado.getId());
    }
}
