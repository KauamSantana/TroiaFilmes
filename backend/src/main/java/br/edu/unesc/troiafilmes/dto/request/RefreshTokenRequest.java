package br.edu.unesc.troiafilmes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Refresh token emitido no login")
public record RefreshTokenRequest(

        @NotBlank(message = "o refresh token é obrigatório")
        String refreshToken
) {
}
