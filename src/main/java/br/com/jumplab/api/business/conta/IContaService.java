package br.com.jumplab.api.business.conta;

public interface IContaService {

    SessaoDTO cadastrar(CadastroRequest pedido);

    SessaoDTO entrar(EntrarRequest pedido);

    SessaoDTO renovar(RenovarRequest pedido);

    void sair(RenovarRequest pedido);

    SessaoDTO criarSenhaNova(Long usuarioId, SenhaNovaRequest pedido);
}
