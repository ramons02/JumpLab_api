package br.com.jumplab.api.business.recuperacao;

import br.com.jumplab.api.core.repositories.IGenericRepository;

import java.util.List;

public interface IPedidoRecuperacaoRepository extends IGenericRepository<PedidoRecuperacaoModel> {

    /** Cria o pedido aberto do usuário ou, se já houver um, só atualiza a data do pedido. */
    void abrirOuAtualizar(Long usuarioId);

    /** Todos os pedidos com o e-mail da conta, do mais novo para o mais antigo. */
    List<PedidoRecuperacaoDTO> listarComEmail();

    /** Marca como atendido só se ainda estiver aberto; falso quando já estava atendido. */
    boolean marcarAtendido(Long id);
}
