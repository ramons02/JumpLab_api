package br.com.jumplab.api.core.exceptions;

/** Pedido sem identidade válida: vira 401 com a mensagem da exceção. */
public abstract class NaoAutenticadoException extends BaseException {

    protected NaoAutenticadoException(String message) {
        super(message);
    }
}
