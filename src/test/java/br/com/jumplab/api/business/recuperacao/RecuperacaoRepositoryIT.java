package br.com.jumplab.api.business.recuperacao;

import br.com.jumplab.api.core.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecuperacaoRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private IPedidoRecuperacaoRepository pedidos;
    @Autowired
    private ISenhaTemporariaRepository senhas;

    private long maria;
    private long admin;

    @BeforeEach
    void preparar() {
        jdbc.update("TRUNCATE senha_temporaria, pedido_recuperacao, token_renovacao, usuario RESTART IDENTITY CASCADE");
        maria = usuario("maria@exemplo.com");
        admin = usuario("admin@exemplo.com");
    }

    private long usuario(String email) {
        return jdbc.queryForObject(
            "INSERT INTO usuario (nome, email, senha_hash) VALUES ('Nome', ?, 'hash') RETURNING id", Long.class, email);
    }

    private long pedidoAberto() {
        return jdbc.queryForObject("SELECT id FROM pedido_recuperacao WHERE status = 'aberto'", Long.class);
    }

    @Test
    void abrirOuAtualizarMantemUmPedidoAbertoEAtualizaAData() {
        pedidos.abrirOuAtualizar(maria);
        jdbc.update("UPDATE pedido_recuperacao SET pedido_em = now() - interval '1 hour'");
        pedidos.abrirOuAtualizar(maria);

        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM pedido_recuperacao", Integer.class));
        assertTrue(jdbc.queryForObject(
            "SELECT pedido_em > now() - interval '1 minute' FROM pedido_recuperacao", Boolean.class));
    }

    @Test
    void indiceParcialRecusaDoisAbertosDoMesmoUsuario() {
        pedidos.abrirOuAtualizar(maria);
        assertThrows(DataIntegrityViolationException.class,
            () -> jdbc.update("INSERT INTO pedido_recuperacao (usuario_id) VALUES (?)", maria));
    }

    @Test
    void marcarAtendidoSoMudaPedidoAbertoELiberaUmNovo() {
        pedidos.abrirOuAtualizar(maria);
        long id = pedidoAberto();

        assertTrue(pedidos.marcarAtendido(id));
        assertFalse(pedidos.marcarAtendido(id));
        pedidos.abrirOuAtualizar(maria);
        assertEquals(2, pedidos.listarComEmail().size());
        assertEquals("aberto", pedidos.listarComEmail().getFirst().status());
        assertEquals("maria@exemplo.com", pedidos.listarComEmail().getFirst().email());
    }

    @Test
    void validasIgnoramVencidasUsadasEInvalidadas() {
        pedidos.abrirOuAtualizar(maria);
        long pedido = pedidoAberto();
        long valida = salvarSenha(pedido, Duration.ofHours(24)).getId();
        salvarSenha(pedido, Duration.ofHours(-1));
        long usada = salvarSenha(pedido, Duration.ofHours(24)).getId();
        senhas.marcarUsada(usada);

        assertEquals(java.util.List.of(valida),
            senhas.listarValidasDoUsuario(maria).stream().map(SenhaTemporariaModel::getId).toList());
        assertFalse(senhas.marcarUsada(usada));

        senhas.invalidarAbertasDoUsuario(maria);
        assertTrue(senhas.listarValidasDoUsuario(maria).isEmpty());
    }

    private SenhaTemporariaModel salvarSenha(long pedido, Duration validade) {
        SenhaTemporariaModel s = new SenhaTemporariaModel();
        s.setUsuarioId(maria);
        s.setPedidoId(pedido);
        s.setSenhaHash("hash");
        s.setGeradaPor(admin);
        Instant agora = Instant.now();
        s.setGeradaEm(agora);
        s.setExpiraEm(agora.plus(validade));
        return senhas.salvar(s);
    }
}
