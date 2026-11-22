package br.com.jumplab.api.core.exceptions;

import java.io.IOException;

/** Corpo da requisição acima do limite da rota (sincronização: 2 MB). Vira 413. */
public class CorpoGrandeDemaisException extends IOException {

    public static final String MENSAGEM = "Requisição grande demais.";

    public CorpoGrandeDemaisException() {
        super(MENSAGEM);
    }
}
