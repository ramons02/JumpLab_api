package br.com.jumplab.api.core.exceptions;

import br.com.jumplab.api.core.controllers.response.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler tratador = new GlobalExceptionHandler();

    @Test
    void erroInesperadoVira500SemAMensagemOriginal() {
        ResponseEntity<ApiResponse<Void>> resposta =
            tratador.handleGeneric(new IllegalStateException("relation \"usuario\" does not exist"));

        assertEquals(500, resposta.getStatusCode().value());
        assertFalse(resposta.getBody().isSucesso());
        assertEquals(GlobalExceptionHandler.MENSAGEM_ERRO_INTERNO, resposta.getBody().getMensagem());
        assertEquals(java.util.List.of(GlobalExceptionHandler.MENSAGEM_ERRO_INTERNO), resposta.getBody().getErros());
    }

    @Test
    void rotaInexistenteVira404ComMensagemPropria() {
        ResponseEntity<ApiResponse<Void>> resposta =
            tratador.handleGeneric(new NoResourceFoundException(HttpMethod.GET, "/api/nada", "api/nada"));

        assertEquals(404, resposta.getStatusCode().value());
        assertEquals(GlobalExceptionHandler.MENSAGEM_NAO_ENCONTRADO, resposta.getBody().getMensagem());
    }

    @Test
    void erroDeCampoVira400ComAMensagemDoCampo() {
        ResponseEntity<ApiResponse<Void>> resposta =
            tratador.handleFieldValidation(new FieldValidationException("nome", "Informe seu nome."));

        assertEquals(400, resposta.getStatusCode().value());
        assertEquals("Informe seu nome.", resposta.getBody().getMensagem());
    }

    @Test
    void regraDeNegocioVira422() {
        ResponseEntity<ApiResponse<Void>> resposta =
            tratador.handleRegra(new BusinessException("Já existe conta com este e-mail."));

        assertEquals(422, resposta.getStatusCode().value());
        assertEquals("Já existe conta com este e-mail.", resposta.getBody().getMensagem());
    }

    @Test
    void credenciaisInvalidasVira401() {
        ResponseEntity<ApiResponse<Void>> resposta =
            tratador.handleNaoAutenticado(new CredenciaisInvalidasException());

        assertEquals(401, resposta.getStatusCode().value());
        assertEquals("E-mail ou senha inválidos.", resposta.getBody().getMensagem());
    }

    @Test
    void acessoNegadoVira403() {
        ResponseEntity<ApiResponse<Void>> resposta = tratador.handleAcessoNegado(
            new org.springframework.security.access.AccessDeniedException("sem papel"));

        assertEquals(403, resposta.getStatusCode().value());
        assertEquals(GlobalExceptionHandler.MENSAGEM_ACESSO_NEGADO, resposta.getBody().getMensagem());
    }
}
