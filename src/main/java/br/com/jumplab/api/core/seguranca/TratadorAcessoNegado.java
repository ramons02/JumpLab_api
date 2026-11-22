package br.com.jumplab.api.core.seguranca;

import br.com.jumplab.api.core.controllers.response.ApiResponse;
import br.com.jumplab.api.core.exceptions.GlobalExceptionHandler;
import br.com.jumplab.api.core.exceptions.SessaoInvalidaException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Respostas 401 e 403 dos filtros do Spring Security, no mesmo envelope do
 * {@link GlobalExceptionHandler} (os filtros rodam antes dos controllers).
 */
@Component
public class TratadorAcessoNegado implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper json;

    public TratadorAcessoNegado(ObjectMapper json) {
        this.json = json;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException e) throws IOException {
        escrever(response, HttpStatus.UNAUTHORIZED, SessaoInvalidaException.MENSAGEM);
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException e) throws IOException {
        escrever(response, HttpStatus.FORBIDDEN, GlobalExceptionHandler.MENSAGEM_ACESSO_NEGADO);
    }

    private void escrever(HttpServletResponse response, HttpStatus status, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        json.writeValue(response.getOutputStream(), ApiResponse.erro(mensagem));
    }
}
