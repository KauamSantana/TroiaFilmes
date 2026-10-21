package br.edu.unesc.troiafilmes.dto.response;

import br.edu.unesc.troiafilmes.entity.StatusAssinatura;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Assinatura de um usuário")
public record AssinaturaResponse(

        Long id,
        Long usuarioId,
        PlanoResponse plano,
        LocalDate dataInicio,
        LocalDate dataFim,
        StatusAssinatura status
) {
}
