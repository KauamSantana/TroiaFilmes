package br.edu.unesc.troiafilmes.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Formato único de erro devolvido pela API, montado pelo
 * {@code GlobalExceptionHandler}.
 */
@Schema(description = "Resposta padronizada de erro")
public record ErroResponse(

        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        @Schema(description = "Momento em que o erro ocorreu")
        LocalDateTime timestamp,

        @Schema(description = "Código HTTP", example = "409")
        int status,

        @Schema(description = "Nome do status HTTP", example = "Conflict")
        String erro,

        @Schema(description = "Mensagem explicativa", example = "Este perfil já avaliou o filme informado.")
        String mensagem,

        @Schema(description = "Caminho da requisição", example = "/api/avaliacoes")
        String caminho,

        @Schema(description = "Erros de validação por campo, quando houver")
        List<ErroCampoResponse> campos
) {

    public static ErroResponse de(int status, String erro, String mensagem, String caminho) {
        return new ErroResponse(LocalDateTime.now(), status, erro, mensagem, caminho, null);
    }

    public static ErroResponse comCampos(int status, String erro, String mensagem,
                                         String caminho, List<ErroCampoResponse> campos) {
        return new ErroResponse(LocalDateTime.now(), status, erro, mensagem, caminho, campos);
    }
}
