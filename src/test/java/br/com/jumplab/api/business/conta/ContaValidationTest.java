package br.com.jumplab.api.business.conta;

import br.com.jumplab.api.core.exceptions.FieldValidationException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ContaValidationTest {

    private final ContaValidation validacao = new ContaValidation();

    @Test
    void dadosValidosPassam() {
        assertDoesNotThrow(() -> validacao.validar(new CadastroRequest("Ana Souza", "ana@exemplo.com", "segredo123")));
    }

    @Test
    void senhaComOitoCaracteresExatosPassa() {
        assertDoesNotThrow(() -> validacao.validar(new CadastroRequest("Ana", "ana@exemplo.com", "12345678")));
    }

    @Test
    void nomeVazioOuCurto() {
        assertErro("nome", "Informe seu nome.", new CadastroRequest(null, "ana@exemplo.com", "segredo123"));
        assertErro("nome", "Informe seu nome.", new CadastroRequest("A", "ana@exemplo.com", "segredo123"));
    }

    @Test
    void nomeLongoDemais() {
        assertErro("nome", "O nome pode ter no máximo 120 caracteres.",
            new CadastroRequest("A".repeat(121), "ana@exemplo.com", "segredo123"));
    }

    @Test
    void emailForaDoFormato() {
        assertErro("email", "Informe um e-mail válido.", new CadastroRequest("Ana", null, "segredo123"));
        assertErro("email", "Informe um e-mail válido.", new CadastroRequest("Ana", "ana.exemplo.com", "segredo123"));
        assertErro("email", "Informe um e-mail válido.", new CadastroRequest("Ana", "ana@exemplo", "segredo123"));
        assertErro("email", "Informe um e-mail válido.",
            new CadastroRequest("Ana", "a".repeat(250) + "@x.com", "segredo123"));
    }

    @Test
    void senhaComSeteCaracteres() {
        assertErro("senha", "A senha precisa ter pelo menos 8 caracteres.",
            new CadastroRequest("Ana", "ana@exemplo.com", "1234567"));
        assertErro("senha", "A senha precisa ter pelo menos 8 caracteres.",
            new CadastroRequest("Ana", "ana@exemplo.com", null));
    }

    @Test
    void senhaAcimaDoLimiteDoBcrypt() {
        assertErro("senha", "A senha pode ter no máximo 72 caracteres.",
            new CadastroRequest("Ana", "ana@exemplo.com", "a".repeat(73)));
    }

    private void assertErro(String campo, String mensagem, CadastroRequest pedido) {
        FieldValidationException erro = assertThrows(FieldValidationException.class, () -> validacao.validar(pedido));
        assertEquals(campo, erro.getField());
        assertEquals(mensagem, erro.getMessage());
    }
}
