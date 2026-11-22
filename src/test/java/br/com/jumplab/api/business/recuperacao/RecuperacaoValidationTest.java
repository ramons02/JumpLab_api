package br.com.jumplab.api.business.recuperacao;

import br.com.jumplab.api.business.conta.ContaValidation;
import br.com.jumplab.api.core.exceptions.FieldValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RecuperacaoValidationTest {

    private final RecuperacaoValidation validacao = new RecuperacaoValidation();

    @Test
    void emailDoPedido() {
        assertDoesNotThrow(() -> validacao.validar(new PedidoRequest("maria@exemplo.com")));
        var erro = assertThrows(FieldValidationException.class, () -> validacao.validar(new PedidoRequest("maria")));
        assertEquals("Informe um e-mail válido.", erro.getMessage());
        assertThrows(FieldValidationException.class, () -> validacao.validar(null));
    }

    @Test
    void senhaNovaCom7E8Caracteres() {
        var erro = assertThrows(FieldValidationException.class, () -> ContaValidation.validarSenha("1234567"));
        assertEquals("A senha precisa ter pelo menos 8 caracteres.", erro.getMessage());
        assertDoesNotThrow(() -> ContaValidation.validarSenha("12345678"));
    }
}
