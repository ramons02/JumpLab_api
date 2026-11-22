package br.com.jumplab.api.core.exceptions;

import br.com.jumplab.api.core.controllers.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Todos os erros saem no envelope {@link ApiResponse}. Detalhe interno (SQL, classe, pilha) fica só no
 * log do servidor, nunca na resposta.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    public static final String MENSAGEM_ERRO_INTERNO = "Erro interno. Tente de novo mais tarde.";
    public static final String MENSAGEM_REQUISICAO_INVALIDA = "Requisição inválida.";
    public static final String MENSAGEM_NAO_ENCONTRADO = "Recurso não encontrado.";
    public static final String MENSAGEM_ACESSO_NEGADO = "Acesso negado.";

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(FieldValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleFieldValidation(FieldValidationException e) {
        return resposta(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler({BusinessException.class, NegocioException.class, RuleValidationException.class})
    public ResponseEntity<ApiResponse<Void>> handleRegra(RuntimeException e) {
        return resposta(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
    }

    @ExceptionHandler(EntidadeNaoEncontradaException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(EntidadeNaoEncontradaException e) {
        return resposta(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(NaoAutenticadoException.class)
    public ResponseEntity<ApiResponse<Void>> handleNaoAutenticado(NaoAutenticadoException e) {
        return resposta(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAcessoNegado(AccessDeniedException e) {
        return resposta(HttpStatus.FORBIDDEN, MENSAGEM_ACESSO_NEGADO);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleCorpoIlegivel(HttpMessageNotReadableException e) {
        if (causadaPorCorpoGrande(e)) {
            return resposta(HttpStatus.CONTENT_TOO_LARGE, CorpoGrandeDemaisException.MENSAGEM);
        }
        return resposta(HttpStatus.BAD_REQUEST, MENSAGEM_REQUISICAO_INVALIDA);
    }

    @ExceptionHandler(CorpoGrandeDemaisException.class)
    public ResponseEntity<ApiResponse<Void>> handleCorpoGrande(CorpoGrandeDemaisException e) {
        return resposta(HttpStatus.CONTENT_TOO_LARGE, CorpoGrandeDemaisException.MENSAGEM);
    }

    private static boolean causadaPorCorpoGrande(Throwable e) {
        for (Throwable causa = e; causa != null; causa = causa.getCause()) {
            if (causa instanceof CorpoGrandeDemaisException) return true;
        }
        return false;
    }

    /**
     * Erros do próprio Spring MVC (rota inexistente, método errado, tipo de parâmetro) trazem o status
     * certo; qualquer outra exceção vira 500 com a mensagem genérica.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneric(Exception e) {
        if (e instanceof ErrorResponse erroDoSpring && erroDoSpring.getStatusCode().is4xxClientError()) {
            HttpStatusCode status = erroDoSpring.getStatusCode();
            String mensagem = status.value() == HttpStatus.NOT_FOUND.value()
                ? MENSAGEM_NAO_ENCONTRADO
                : MENSAGEM_REQUISICAO_INVALIDA;
            return resposta(status, mensagem);
        }
        log.error("Erro inesperado ao atender a requisição", e);
        return resposta(HttpStatus.INTERNAL_SERVER_ERROR, MENSAGEM_ERRO_INTERNO);
    }

    private static ResponseEntity<ApiResponse<Void>> resposta(HttpStatusCode status, String mensagem) {
        return ResponseEntity.status(status).body(ApiResponse.erro(mensagem));
    }
}
