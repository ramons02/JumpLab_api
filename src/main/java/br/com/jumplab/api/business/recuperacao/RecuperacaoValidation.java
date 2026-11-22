package br.com.jumplab.api.business.recuperacao;

import br.com.jumplab.api.business.conta.ContaValidation;
import br.com.jumplab.api.core.validations.GenericValidation;
import org.springframework.stereotype.Component;

/** E-mail do pedido de recuperação, com a mesma regra do cadastro (spec 012). */
@Component
public class RecuperacaoValidation extends GenericValidation<PedidoRequest> implements IRecuperacaoValidation {

    @Override
    public void validar(PedidoRequest pedido) {
        ContaValidation.exigirEmailValido(pedido == null ? null : pedido.email());
    }
}
