package br.edu.unesc.troiafilmes.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Atualização de dados cadastrais. A senha é opcional: quando ausente,
 * a atual é mantida.
 */
@Schema(description = "Dados cadastrais do usuário")
public record AtualizarUsuarioRequest(

        @NotBlank(message = "o nome é obrigatório")
        @Size(min = 3, max = 120, message = "o nome deve ter entre 3 e 120 caracteres")
        String nome,

        @NotBlank(message = "o e-mail é obrigatório")
        @Email(message = "informe um e-mail válido")
        @Size(max = 160, message = "o e-mail deve ter no máximo 160 caracteres")
        String email,

        @Size(min = 8, max = 72, message = "a senha deve ter entre 8 e 72 caracteres")
        @Schema(description = "Opcional. Informe apenas para trocar a senha.")
        String novaSenha
) {
}
