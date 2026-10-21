package br.edu.unesc.troiafilmes.controller;

import br.edu.unesc.troiafilmes.dto.request.LoginRequest;
import br.edu.unesc.troiafilmes.dto.request.RefreshTokenRequest;
import br.edu.unesc.troiafilmes.dto.request.RegistroUsuarioRequest;
import br.edu.unesc.troiafilmes.dto.response.ErroResponse;
import br.edu.unesc.troiafilmes.dto.response.LoginResponse;
import br.edu.unesc.troiafilmes.dto.response.RegistroUsuarioResponse;
import br.edu.unesc.troiafilmes.entity.Usuario;
import br.edu.unesc.troiafilmes.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/** Cadastro, login e ciclo de vida da sessão. */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "1. Autenticação", description = "Cadastro, login, renovação e encerramento de sessão")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/registrar")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Cria uma conta de assinante",
            description = """
                    Registra um novo usuário com papel USER, já vinculado ao plano Básico e com
                    um perfil "Principal" criado. Depois do cadastro, entre por /api/auth/login.
                    """,
            security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conta criada"),
            @ApiResponse(responseCode = "400", description = "Dados inválidos",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class))),
            @ApiResponse(responseCode = "409", description = "E-mail já cadastrado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    public RegistroUsuarioResponse registrar(@Valid @RequestBody RegistroUsuarioRequest request) {
        return authService.registrar(request);
    }

    @PostMapping("/login")
    @Operation(
            summary = "Confere e-mail e senha e emite os tokens",
            description = "Credenciais de demonstração: admin@troiafilmes.com / Admin@123 e "
                    + "usuario@troiafilmes.com / User@123.",
            security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Autenticado"),
            @ApiResponse(responseCode = "401", description = "E-mail ou senha inválidos, ou conta desativada",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "Renova o token de acesso",
            description = "Troca um refresh token válido por um novo par de tokens. "
                    + "O refresh token enviado é revogado na troca.",
            security = {})
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tokens renovados"),
            @ApiResponse(responseCode = "401", description = "Refresh token inválido, expirado ou revogado",
                    content = @Content(schema = @Schema(implementation = ErroResponse.class)))
    })
    public LoginResponse renovar(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.renovar(request.refreshToken());
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Encerra as sessões da conta",
            description = "Revoga todos os refresh tokens do usuário autenticado. O token de "
                    + "acesso atual continua válido até expirar, por ser autocontido.")
    @ApiResponse(responseCode = "204", description = "Sessões encerradas")
    public void logout(@AuthenticationPrincipal Usuario usuario) {
        authService.logout(usuario.getId());
    }
}
