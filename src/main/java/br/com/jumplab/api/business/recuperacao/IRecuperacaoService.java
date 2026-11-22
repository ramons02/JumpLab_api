package br.com.jumplab.api.business.recuperacao;

import java.util.List;

public interface IRecuperacaoService {

    void pedir(PedidoRequest pedido);

    List<PedidoRecuperacaoDTO> listarPedidos();

    SenhaTemporariaGeradaDTO gerarSenha(Long pedidoId, Long administradorId);
}
