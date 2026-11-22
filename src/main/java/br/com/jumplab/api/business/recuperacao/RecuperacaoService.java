package br.com.jumplab.api.business.recuperacao;

import br.com.jumplab.api.business.conta.ContaService;
import br.com.jumplab.api.business.conta.IUsuarioRepository;
import br.com.jumplab.api.business.conta.UsuarioModel;
import br.com.jumplab.api.core.exceptions.BusinessException;
import br.com.jumplab.api.core.exceptions.EntidadeNaoEncontradaException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/** Recuperação de senha pelo administrador, sem e-mail (D13). */
@Service
public class RecuperacaoService implements IRecuperacaoService {

    static final String MENSAGEM_PEDIDO_NAO_ENCONTRADO = "Pedido não encontrado.";
    static final String MENSAGEM_PEDIDO_ATENDIDO = "Pedido já atendido.";
    static final Duration VALIDADE_SENHA_TEMPORARIA = Duration.ofHours(24);

    private final IPedidoRecuperacaoRepository pedidos;
    private final ISenhaTemporariaRepository senhas;
    private final IUsuarioRepository usuarios;
    private final IRecuperacaoValidation validacao;
    private final PasswordEncoder codificadorDeSenha;
    private final GeradorSenhaTemporaria gerador;

    public RecuperacaoService(IPedidoRecuperacaoRepository pedidos, ISenhaTemporariaRepository senhas,
                              IUsuarioRepository usuarios, IRecuperacaoValidation validacao,
                              PasswordEncoder codificadorDeSenha, GeradorSenhaTemporaria gerador) {
        this.pedidos = pedidos;
        this.senhas = senhas;
        this.usuarios = usuarios;
        this.validacao = validacao;
        this.codificadorDeSenha = codificadorDeSenha;
        this.gerador = gerador;
    }

    /** Só grava o pedido de conta ativa; para quem pede, a resposta é sempre a mesma (RN02). */
    @Override
    @Transactional
    public void pedir(PedidoRequest pedido) {
        String email = ContaService.normalizarEmail(pedido == null ? null : pedido.email());
        validacao.validar(new PedidoRequest(email));
        usuarios.buscarPorEmail(email)
            .filter(UsuarioModel::isAtivo)
            .ifPresent(usuario -> pedidos.abrirOuAtualizar(usuario.getId()));
    }

    @Override
    public List<PedidoRecuperacaoDTO> listarPedidos() {
        return pedidos.listarComEmail();
    }

    /**
     * Numa transação: invalida as temporárias abertas do usuário, grava só o hash da nova com quem gerou e
     * quando, e marca o pedido atendido. O texto da senha só sai nesta resposta.
     */
    @Override
    @Transactional
    public SenhaTemporariaGeradaDTO gerarSenha(Long pedidoId, Long administradorId) {
        PedidoRecuperacaoModel pedido = pedidos.buscarPorId(pedidoId)
            .orElseThrow(() -> new EntidadeNaoEncontradaException(MENSAGEM_PEDIDO_NAO_ENCONTRADO));
        if (!PedidoRecuperacaoModel.ABERTO.equals(pedido.getStatus())) {
            throw new BusinessException(MENSAGEM_PEDIDO_ATENDIDO);
        }
        UsuarioModel usuario = usuarios.buscarPorId(pedido.getUsuarioId())
            .orElseThrow(() -> new EntidadeNaoEncontradaException(MENSAGEM_PEDIDO_NAO_ENCONTRADO));

        String texto = gerador.gerar();
        Instant agora = Instant.now();
        senhas.invalidarAbertasDoUsuario(usuario.getId());
        SenhaTemporariaModel registro = new SenhaTemporariaModel();
        registro.setUsuarioId(usuario.getId());
        registro.setPedidoId(pedido.getId());
        registro.setSenhaHash(codificadorDeSenha.encode(texto));
        registro.setGeradaPor(administradorId);
        registro.setGeradaEm(agora);
        registro.setExpiraEm(agora.plus(VALIDADE_SENHA_TEMPORARIA));
        senhas.salvar(registro);
        if (!pedidos.marcarAtendido(pedido.getId())) {
            throw new BusinessException(MENSAGEM_PEDIDO_ATENDIDO);
        }
        return new SenhaTemporariaGeradaDTO(pedido.getId(), usuario.getEmail(), texto, registro.getExpiraEm());
    }
}
