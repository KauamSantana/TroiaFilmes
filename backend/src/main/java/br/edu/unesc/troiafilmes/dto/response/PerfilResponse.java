package br.edu.unesc.troiafilmes.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "Perfil de consumo de uma conta")
public record PerfilResponse(

        Long id,
        Long usuarioId,
        String nome,
        String avatarUrl,
        Boolean infantil,
        LocalDateTime criadoEm
) {
}
