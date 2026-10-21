package br.edu.unesc.troiafilmes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Devolvido no login e na renovação da sessão. */
@Schema(description = "Tokens emitidos pela API e dados de quem entrou")
public record LoginResponse(

        @Schema(description = "JWT que vai no header Authorization: Bearer <token>")
        String token,

        @Schema(description = "Token de longa duração, trocado por um novo par em /api/auth/refresh")
        String refreshToken,

        @Schema(description = "Esquema de autenticação", example = "Bearer")
        String tipo,

        @Schema(description = "Validade do token, em segundos", example = "3600")
        long expiraEmSegundos,

        @Schema(description = "Dados do usuário autenticado")
        UsuarioResponse usuario
) {

    public static LoginResponse de(String token, String refreshToken,
                                   long expiraEmSegundos, UsuarioResponse usuario) {
        return new LoginResponse(token, refreshToken, "Bearer", expiraEmSegundos, usuario);
    }
}
