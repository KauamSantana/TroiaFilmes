package br.edu.unesc.troiafilmes.controller;

import br.edu.unesc.troiafilmes.dto.request.AtualizarUsuarioRequest;
import br.edu.unesc.troiafilmes.dto.response.UsuarioResponse;
import br.edu.unesc.troiafilmes.entity.Usuario;
import br.edu.unesc.troiafilmes.service.UsuarioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Dados da conta do próprio usuário autenticado.
 *
 * <p>Fica separado de {@code /api/usuarios} de propósito: aquele caminho é
 * exclusivo de ADMIN, e o usuário comum precisa alcançar os próprios dados
 * sem receber essa permissão.</p>
 */
@RestController
@RequestMapping("/api/conta")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "3. Minha conta", description = "Dados cadastrais do usuário autenticado")
public class ContaController {

    private final UsuarioService usuarioService;

    @GetMapping
    @Operation(summary = "Dados da conta autenticada")
    public UsuarioResponse buscar(@AuthenticationPrincipal Usuario autenticado) {
        return usuarioService.buscarPerfilProprio(autenticado.getId());
    }

    @PutMapping
    @Operation(
            summary = "Atualiza nome, e-mail e, opcionalmente, a senha",
            description = "Trocar a senha encerra as sessões abertas da conta.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dados atualizados"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos"),
            @ApiResponse(responseCode = "409", description = "E-mail já usado por outra conta")
    })
    public UsuarioResponse atualizar(@AuthenticationPrincipal Usuario autenticado,
                                     @Valid @RequestBody AtualizarUsuarioRequest request) {
        return usuarioService.atualizarPropriosDados(autenticado.getId(), request);
    }
}
