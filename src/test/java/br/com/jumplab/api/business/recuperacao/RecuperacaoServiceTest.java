package br.com.jumplab.api.business.recuperacao;

import br.com.jumplab.api.business.conta.IUsuarioRepository;
import br.com.jumplab.api.business.conta.UsuarioModel;
import br.com.jumplab.api.core.exceptions.BusinessException;
import br.com.jumplab.api.core.exceptions.EntidadeNaoEncontradaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecuperacaoServiceTest {

    private final IPedidoRecuperacaoRepository pedidos = mock(IPedidoRecuperacaoRepository.class);
    private final ISenhaTemporariaRepository senhas = mock(ISenhaTemporariaRepository.class);
    private final IUsuarioRepository usuarios = mock(IUsuarioRepository.class);
    private final PasswordEncoder codificador = mock(PasswordEncoder.class);
    private final GeradorSenhaTemporaria gerador = mock(GeradorSenhaTemporaria.class);
    private RecuperacaoService servico;

    @BeforeEach
    void preparar() {
        when(codificador.encode(anyString())).thenAnswer(inv -> "bcrypt(" + inv.getArgument(0) + ")");
        when(gerador.gerar()).thenReturn("K7QM-3WZP");
        when(senhas.salvar(any())).thenAnswer(inv -> inv.getArgument(0));
        servico = new RecuperacaoService(pedidos, senhas, usuarios, new RecuperacaoValidation(), codificador, gerador);
    }

    // pedir

    @Test
    void comContaCriaOuAtualizaOPedido() {
        when(usuarios.buscarPorEmail("maria@exemplo.com")).thenReturn(Optional.of(maria(true)));
        servico.pedir(new PedidoRequest(" Maria@Exemplo.com "));
        verify(pedidos).abrirOuAtualizar(7L);
    }

    @Test
    void semContaNaoGravaNadaENaoLanca() {
        when(usuarios.buscarPorEmail(anyString())).thenReturn(Optional.empty());
        servico.pedir(new PedidoRequest("ninguem@exemplo.com"));
        verify(pedidos, never()).abrirOuAtualizar(anyLong());
    }

    @Test
    void contaInativaNaoGrava() {
        when(usuarios.buscarPorEmail("maria@exemplo.com")).thenReturn(Optional.of(maria(false)));
        servico.pedir(new PedidoRequest("maria@exemplo.com"));
        verify(pedidos, never()).abrirOuAtualizar(anyLong());
    }

    // gerar senha

    @Test
    void gravaSoOHashComQuemGerouEValidadeDe24h() {
        when(pedidos.buscarPorId(12L)).thenReturn(Optional.of(pedido("aberto")));
        when(usuarios.buscarPorId(7L)).thenReturn(Optional.of(maria(true)));
        when(pedidos.marcarAtendido(12L)).thenReturn(true);

        SenhaTemporariaGeradaDTO gerada = servico.gerarSenha(12L, 1L);

        ArgumentCaptor<SenhaTemporariaModel> registro = ArgumentCaptor.forClass(SenhaTemporariaModel.class);
        var ordem = inOrder(senhas, pedidos);
        ordem.verify(senhas).invalidarAbertasDoUsuario(7L);
        ordem.verify(senhas).salvar(registro.capture());
        ordem.verify(pedidos).marcarAtendido(12L);
        assertEquals("bcrypt(K7QM-3WZP)", registro.getValue().getSenhaHash());
        assertFalse(registro.getValue().getSenhaHash().equals("K7QM-3WZP"));
        assertEquals(1L, registro.getValue().getGeradaPor());
        assertEquals(Duration.ofHours(24),
            Duration.between(registro.getValue().getGeradaEm(), registro.getValue().getExpiraEm()));
        assertEquals(new SenhaTemporariaGeradaDTO(12L, "maria@exemplo.com", "K7QM-3WZP",
            registro.getValue().getExpiraEm()), gerada);
        verify(usuarios, never()).atualizar(any());
    }

    @Test
    void pedidoInexistente() {
        when(pedidos.buscarPorId(99L)).thenReturn(Optional.empty());
        var erro = assertThrows(EntidadeNaoEncontradaException.class, () -> servico.gerarSenha(99L, 1L));
        assertEquals("Pedido não encontrado.", erro.getMessage());
    }

    @Test
    void pedidoJaAtendido() {
        when(pedidos.buscarPorId(12L)).thenReturn(Optional.of(pedido("atendido")));
        var erro = assertThrows(BusinessException.class, () -> servico.gerarSenha(12L, 1L));
        assertEquals("Pedido já atendido.", erro.getMessage());
        verify(senhas, never()).salvar(any());
    }

    private static UsuarioModel maria(boolean ativa) {
        UsuarioModel u = new UsuarioModel();
        u.setId(7L);
        u.setEmail("maria@exemplo.com");
        u.setAtivo(ativa);
        return u;
    }

    private static PedidoRecuperacaoModel pedido(String status) {
        PedidoRecuperacaoModel p = new PedidoRecuperacaoModel();
        p.setId(12L);
        p.setUsuarioId(7L);
        p.setStatus(status);
        return p;
    }
}
