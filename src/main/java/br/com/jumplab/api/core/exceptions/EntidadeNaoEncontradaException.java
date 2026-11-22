package br.com.jumplab.api.core.exceptions;

public class EntidadeNaoEncontradaException extends RuntimeException {
    public EntidadeNaoEncontradaException(String entidade, Long id) {
        super(entidade + " com id " + id + " não encontrado(a)");
    }

    /** Com a mensagem exata do contrato (ex.: "Pedido não encontrado."). */
    public EntidadeNaoEncontradaException(String mensagem) {
        super(mensagem);
    }
}
