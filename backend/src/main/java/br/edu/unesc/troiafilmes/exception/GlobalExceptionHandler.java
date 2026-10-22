package br.edu.unesc.troiafilmes.exception;

import br.edu.unesc.troiafilmes.dto.response.ErroCampoResponse;
import br.edu.unesc.troiafilmes.dto.response.ErroResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Comparator;
import java.util.List;

/**
 * Tratamento centralizado de exceções da API.
 *
 * <p>Concentrar o tratamento aqui mantém os controllers limpos e garante que
 * toda falha, esperada ou não, saia no mesmo formato ({@link ErroResponse}).</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ------------------------------------------------- exceções de domínio

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResponse> tratarRecursoNaoEncontrado(
            RecursoNaoEncontradoException ex, HttpServletRequest req) {
        log.warn("Recurso não encontrado em {}: {}", req.getRequestURI(), ex.getMessage());
        return montar(HttpStatus.NOT_FOUND, ex.getMessage(), req);
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErroResponse> tratarRegraDeNegocio(
            RegraDeNegocioException ex, HttpServletRequest req) {
        log.warn("Regra de negócio violada em {}: {}", req.getRequestURI(), ex.getMessage());
        return montar(HttpStatus.CONFLICT, ex.getMessage(), req);
    }

    @ExceptionHandler(ArquivoInvalidoException.class)
    public ResponseEntity<ErroResponse> tratarArquivoInvalido(
            ArquivoInvalidoException ex, HttpServletRequest req) {
        log.warn("Upload recusado em {}: {}", req.getRequestURI(), ex.getMessage());
        return montar(HttpStatus.BAD_REQUEST, ex.getMessage(), req);
    }

    @ExceptionHandler(AcessoNegadoException.class)
    public ResponseEntity<ErroResponse> tratarAcessoNegado(
            AcessoNegadoException ex, HttpServletRequest req) {
        log.warn("Acesso negado em {}: {}", req.getRequestURI(), ex.getMessage());
        return montar(HttpStatus.FORBIDDEN, ex.getMessage(), req);
    }

    // ------------------------------------------------------------ validação

    /** Erros de {@code @Valid} no corpo da requisição. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> tratarValidacao(
            MethodArgumentNotValidException ex, HttpServletRequest req) {

        List<ErroCampoResponse> campos = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ErroCampoResponse(e.getField(), e.getDefaultMessage()))
                .sorted(Comparator.comparing(ErroCampoResponse::campo))
                .toList();

        log.warn("Validação falhou em {}: {} campo(s) inválido(s)",
                req.getRequestURI(), campos.size());

        return ResponseEntity.badRequest().body(ErroResponse.comCampos(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Um ou mais campos estão inválidos. Verifique a lista em 'campos'.",
                req.getRequestURI(),
                campos));
    }

    /** Erros de {@code @Validated} em parâmetros de método (query/path). */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErroResponse> tratarConstraint(
            ConstraintViolationException ex, HttpServletRequest req) {

        List<ErroCampoResponse> campos = ex.getConstraintViolations().stream()
                .map(v -> new ErroCampoResponse(String.valueOf(v.getPropertyPath()), v.getMessage()))
                .sorted(Comparator.comparing(ErroCampoResponse::campo))
                .toList();

        return ResponseEntity.badRequest().body(ErroResponse.comCampos(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Parâmetros inválidos na requisição.",
                req.getRequestURI(),
                campos));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> tratarCorpoIlegivel(
            HttpMessageNotReadableException ex, HttpServletRequest req) {
        return montar(HttpStatus.BAD_REQUEST,
                "Corpo da requisição ausente ou mal formatado. Envie um JSON válido.", req);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErroResponse> tratarTipoInvalido(
            MethodArgumentTypeMismatchException ex, HttpServletRequest req) {
        return montar(HttpStatus.BAD_REQUEST,
                "O parâmetro %s recebeu um valor incompatível: %s."
                        .formatted(ex.getName(), ex.getValue()), req);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErroResponse> tratarParametroAusente(
            MissingServletRequestParameterException ex, HttpServletRequest req) {
        return montar(HttpStatus.BAD_REQUEST,
                "O parâmetro obrigatório %s não foi informado."
                        .formatted(ex.getParameterName()), req);
    }

    // ------------------------------------------------------------- segurança

    /**
     * Login recusado ou refresh token inválido. A mensagem vem do
     * {@code AuthService}, que já a escreve sem revelar se o e-mail existe.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErroResponse> tratarCredenciaisInvalidas(
            BadCredentialsException ex, HttpServletRequest req) {
        log.warn("Credenciais recusadas em {}: {}", req.getRequestURI(), ex.getMessage());
        return montar(HttpStatus.UNAUTHORIZED, ex.getMessage(), req);
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErroResponse> tratarContaDesativada(
            DisabledException ex, HttpServletRequest req) {
        return montar(HttpStatus.UNAUTHORIZED,
                "Esta conta está desativada. Procure o suporte do TroiaFilmes.", req);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> tratarAutorizacaoNegada(
            AccessDeniedException ex, HttpServletRequest req) {
        return montar(HttpStatus.FORBIDDEN,
                "Você não tem permissão para executar esta operação.", req);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponse> tratarFalhaDeAutenticacao(
            AuthenticationException ex, HttpServletRequest req) {
        return montar(HttpStatus.UNAUTHORIZED, "Falha na autenticação.", req);
    }

    /**
     * O limite do container corta a requisição antes que ela chegue ao
     * service, então este caso não passa pelo {@link ArquivoInvalidoException}.
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErroResponse> tratarUploadGrandeDemais(
            MaxUploadSizeExceededException ex, HttpServletRequest req) {
        log.warn("Upload acima do limite em {}", req.getRequestURI());
        return montar(HttpStatus.PAYLOAD_TOO_LARGE,
                "A imagem enviada excede o tamanho máximo permitido.", req);
    }

    /** Formulário multipart sem o campo de arquivo esperado. */
    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<ErroResponse> tratarParteAusente(
            MissingServletRequestPartException ex, HttpServletRequest req) {
        return montar(HttpStatus.BAD_REQUEST,
                "O campo de arquivo '%s' não foi enviado.".formatted(ex.getRequestPartName()), req);
    }

    // ---------------------------------------------------------------- outros

    /**
     * Rede de segurança para restrições do banco que escaparem da validação
     * no service, como uma corrida entre duas requisições simultâneas.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErroResponse> tratarIntegridade(
            DataIntegrityViolationException ex, HttpServletRequest req) {
        log.warn("Violação de integridade em {}: {}",
                req.getRequestURI(), ex.getMostSpecificCause().getMessage());
        return montar(HttpStatus.CONFLICT,
                "A operação viola uma restrição de integridade dos dados.", req);
    }

    /** Verbo que a rota nao aceita, como DELETE numa rota que so tem GET e POST. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErroResponse> tratarMetodoNaoSuportado(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest req) {
        return montar(HttpStatus.METHOD_NOT_ALLOWED,
                "O método %s não é aceito em %s.".formatted(ex.getMethod(), req.getRequestURI()), req);
    }

    /** Corpo enviado num formato que a rota nao le (ex.: texto puro no lugar de JSON). */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErroResponse> tratarFormatoNaoSuportado(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest req) {
        return montar(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Formato de conteúdo não suportado. Envie o corpo como JSON.", req);
    }

    /**
     * O navegador desistiu da resposta no meio do caminho, por exemplo ao trocar
     * de pagina enquanto um poster ainda carregava. Nao e erro do servidor, e
     * nao ha mais para quem responder.
     */
    @ExceptionHandler(ClientAbortException.class)
    public void tratarConexaoEncerradaPeloCliente(ClientAbortException ex, HttpServletRequest req) {
        log.debug("Cliente encerrou a conexão em {}", req.getRequestURI());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErroResponse> tratarRotaInexistente(
            NoResourceFoundException ex, HttpServletRequest req) {
        return montar(HttpStatus.NOT_FOUND,
                "Rota não encontrada: %s".formatted(req.getRequestURI()), req);
    }

    /** Último recurso: registra o stack trace e devolve 500 sem vazar detalhes. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> tratarErroInesperado(
            Exception ex, HttpServletRequest req) {
        log.error("Erro inesperado em {}", req.getRequestURI(), ex);
        return montar(HttpStatus.INTERNAL_SERVER_ERROR,
                "Erro interno no servidor. Tente novamente em instantes.", req);
    }

    // -------------------------------------------------------------- privados

    private ResponseEntity<ErroResponse> montar(HttpStatus status, String mensagem,
                                                HttpServletRequest req) {
        return ResponseEntity.status(status).body(ErroResponse.de(
                status.value(), status.getReasonPhrase(), mensagem, req.getRequestURI()));
    }
}
