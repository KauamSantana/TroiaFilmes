package br.edu.unesc.troiafilmes.dto.request;

import br.edu.unesc.troiafilmes.entity.Qualidade;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Dados de um plano de assinatura")
public record PlanoRequest(

        @NotBlank(message = "o nome do plano é obrigatório")
        @Size(min = 2, max = 60, message = "o nome deve ter entre 2 e 60 caracteres")
        @Schema(example = "Premium")
        String nome,

        @Size(max = 255, message = "a descrição deve ter no máximo 255 caracteres")
        String descricao,

        @NotNull(message = "o preço é obrigatório")
        @DecimalMin(value = "0.00", message = "o preço não pode ser negativo")
        @Digits(integer = 6, fraction = 2, message = "o preço deve ter no máximo 2 casas decimais")
        @Schema(example = "54.90")
        BigDecimal preco,

        @NotNull(message = "o limite de perfis é obrigatório")
        @Min(value = 1, message = "o plano deve permitir ao menos 1 perfil")
        @Max(value = 10, message = "o limite máximo é de 10 perfis")
        @Schema(example = "5")
        Integer maxPerfis,

        @NotNull(message = "a qualidade máxima é obrigatória")
        @Schema(example = "UHD")
        Qualidade qualidadeMaxima,

        @Schema(description = "Se o plano está disponível para contratação", example = "true")
        Boolean ativo
) {
}
