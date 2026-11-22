package br.com.jumplab.api.core.exceptions;

public class EntidadeNaoEncontradaException extends RuntimeException {
    public EntidadeNaoEncontradaException(String entidade, Long id) {
        super(entidade + " com id " + id + " não encontrado(a)");
    }
}
