package br.edu.unesc.troiafilmes.config;

import br.edu.unesc.troiafilmes.dto.response.ErroResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Respostas de 401 e 403 originadas dentro da cadeia de filtros.
 *
 * <p>O {@code GlobalExceptionHandler} só enxerga o que passa pelo
 * DispatcherServlet; falhas de autenticação acontecem antes disso. Sem esta
 * classe, o cliente receberia a página de erro padrão do servidor em vez do
 * mesmo JSON usado no resto da API.</p>
 */
@Component
@RequiredArgsConstructor
public class RespostaJsonDeSeguranca implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    /** Requisição sem token, ou com token inválido/expirado. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        escrever(request, response, HttpStatus.UNAUTHORIZED,
                "Autenticação necessária. Envie um token válido no header Authorization.");
    }

    /** Usuário autenticado, mas sem permissão para a operação. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        escrever(request, response, HttpStatus.FORBIDDEN,
                "Você não tem permissão para executar esta operação.");
    }

    private void escrever(HttpServletRequest request, HttpServletResponse response,
                          HttpStatus status, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        ErroResponse corpo = ErroResponse.de(
                status.value(), status.getReasonPhrase(), mensagem, request.getRequestURI());

        objectMapper.writeValue(response.getOutputStream(), corpo);
    }
}
