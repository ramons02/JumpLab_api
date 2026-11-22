package br.com.jumplab.api.core.exceptions;

public class SessaoInvalidaException extends NaoAutenticadoException {

    public static final String MENSAGEM = "Sessão inválida ou expirada.";

    public SessaoInvalidaException() {
        super(MENSAGEM);
    }
}
