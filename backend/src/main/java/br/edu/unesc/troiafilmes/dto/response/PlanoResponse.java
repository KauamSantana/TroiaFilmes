package br.edu.unesc.troiafilmes.dto.response;

import br.edu.unesc.troiafilmes.entity.Qualidade;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Plano de assinatura")
public record PlanoResponse(

        Long id,
        String nome,
        String descricao,
        BigDecimal preco,
        Integer maxPerfis,
        Qualidade qualidadeMaxima,
        Boolean ativo
) {
}
