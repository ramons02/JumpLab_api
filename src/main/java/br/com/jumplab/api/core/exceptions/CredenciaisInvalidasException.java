package br.com.jumplab.api.core.exceptions;

/** Mesma mensagem para e-mail inexistente e senha errada, para não revelar quem tem conta. */
public class CredenciaisInvalidasException extends NaoAutenticadoException {

    public static final String MENSAGEM = "E-mail ou senha inválidos.";

    public CredenciaisInvalidasException() {
        super(MENSAGEM);
    }
}
