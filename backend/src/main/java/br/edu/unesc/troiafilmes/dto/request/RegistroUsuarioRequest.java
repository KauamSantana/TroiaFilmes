package br.edu.unesc.troiafilmes.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Dados de criação de uma conta, enviados em {@code POST /api/auth/registrar}.
 *
 * <p>Os limites de tamanho acompanham as colunas da tabela {@code usuario}.
 * A senha para em 72 caracteres porque o BCrypt ignora o que vem depois.</p>
 */
public record RegistroUsuarioRequest(

        @NotBlank(message = "O nome é obrigatório.")
        @Size(min = 3, max = 120, message = "O nome deve ter entre 3 e 120 caracteres.")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Informe um e-mail válido.")
        @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
        String senha
) {
}
