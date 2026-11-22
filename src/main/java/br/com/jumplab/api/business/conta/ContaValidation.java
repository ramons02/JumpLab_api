package br.com.jumplab.api.business.conta;

import br.com.jumplab.api.core.exceptions.FieldValidationException;
import br.com.jumplab.api.core.validations.GenericValidation;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

/** Regras do cadastro. Recebe o pedido já com nome e e-mail normalizados pelo service. */
@Component
public class ContaValidation extends GenericValidation<CadastroRequest> implements IContaValidation {

    static final String MENSAGEM_NOME = "Informe seu nome.";
    static final String MENSAGEM_NOME_LONGO = "O nome pode ter no máximo 120 caracteres.";
    static final String MENSAGEM_EMAIL = "Informe um e-mail válido.";
    static final String MENSAGEM_SENHA_CURTA = "A senha precisa ter pelo menos 8 caracteres.";
    static final String MENSAGEM_SENHA_LONGA = "A senha pode ter no máximo 72 caracteres.";

    static final int NOME_MINIMO = 2;
    static final int NOME_MAXIMO = 120;
    static final int EMAIL_MAXIMO = 254;
    static final int SENHA_MINIMA = 8;
    /** O BCrypt só considera os primeiros 72 bytes da senha. */
    static final int SENHA_MAXIMA_BYTES = 72;

    private static final Pattern FORMATO_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    @Override
    public void validar(CadastroRequest pedido) {
        validarNome(pedido.nome());
        validarEmail(pedido.email());
        validarSenha(pedido.senha());
    }

    private static void validarNome(String nome) {
        if (nome == null || nome.length() < NOME_MINIMO) {
            throw new FieldValidationException("nome", MENSAGEM_NOME);
        }
        if (nome.length() > NOME_MAXIMO) {
            throw new FieldValidationException("nome", MENSAGEM_NOME_LONGO);
        }
    }

    @Override
    protected void validarEmail(String email) {
        if (email == null || email.length() > EMAIL_MAXIMO || !FORMATO_EMAIL.matcher(email).matches()) {
            throw new FieldValidationException("email", MENSAGEM_EMAIL);
        }
    }

    private static void validarSenha(String senha) {
        if (senha == null || senha.length() < SENHA_MINIMA) {
            throw new FieldValidationException("senha", MENSAGEM_SENHA_CURTA);
        }
        if (senha.getBytes(StandardCharsets.UTF_8).length > SENHA_MAXIMA_BYTES) {
            throw new FieldValidationException("senha", MENSAGEM_SENHA_LONGA);
        }
    }
}
