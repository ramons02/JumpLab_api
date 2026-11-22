package br.com.jumplab.api.business.recuperacao;

import br.com.jumplab.api.core.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static br.com.jumplab.api.core.ContasDeTeste.criarConta;
import static br.com.jumplab.api.core.ContasDeTeste.entrar;
import static br.com.jumplab.api.core.ContasDeTeste.post;
import static br.com.jumplab.api.core.ContasDeTeste.tokenAcesso;
import static br.com.jumplab.api.core.ContasDeTeste.tornarAdmin;
import static br.com.jumplab.api.core.ContasDeTeste.SENHA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecuperacaoControllerIT extends AbstractIntegrationTest {

    private static final String PEDIDOS = "/api/recuperacao/pedidos";
    private static final String ADMIN = "/api/admin/pedidos";

    @BeforeEach
    void limpar() {
        jdbc.update("TRUNCATE senha_temporaria, pedido_recuperacao, token_renovacao, usuario RESTART IDENTITY CASCADE");
        criarConta(restClient, "maria@exemplo.com");
        criarConta(restClient, "admin@exemplo.com");
        tornarAdmin(jdbc, "admin@exemplo.com");
    }

    private String pedir(String email) {
        return post(restClient, PEDIDOS, Map.of("email", email)).expectStatus().isAccepted()
            .expectBody(String.class).returnResult().getResponseBody();
    }

    private int pedidosAbertos() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM pedido_recuperacao WHERE status = 'aberto'", Integer.class);
    }

    private String tokenAdmin() {
        return tokenAcesso(entrar(restClient, "admin@exemplo.com", SENHA));
    }

    // pedido (US1)

    @Test
    void comESemContaRespondem202ComCorposIdenticosESemToken() {
        String comConta = pedir("maria@exemplo.com");
        String semConta = pedir("ninguem@exemplo.com");

        assertEquals(comConta, semConta);
        assertEquals("Pedido enviado. O administrador vai gerar uma senha temporária para você.",
            JsonPath.read(comConta, "$.mensagem"));
        assertEquals(1, pedidosAbertos());
    }

    @Test
    void emailEmMaiusculasAchaAContaEPedidoRepetidoMantemUmAberto() {
        pedir(" MARIA@Exemplo.com ");
        pedir("maria@exemplo.com");
        assertEquals(1, pedidosAbertos());
    }

    @Test
    void emailInvalidoDa400() {
        post(restClient, PEDIDOS, Map.of("email", "maria")).expectStatus().isBadRequest()
            .expectBody().jsonPath("$.mensagem").isEqualTo("Informe um e-mail válido.");
    }

    // caixa do administrador (US2)

    @Test
    void semToken401EContaComum403() {
        restClient.get().uri(ADMIN).exchange().expectStatus().isUnauthorized();
        String comum = tokenAcesso(entrar(restClient, "maria@exemplo.com", SENHA));
        restClient.get().uri(ADMIN).header("Authorization", "Bearer " + comum).exchange()
            .expectStatus().isForbidden().expectBody().jsonPath("$.mensagem").isEqualTo("Acesso negado.");
        post(restClient, ADMIN + "/1/senha-temporaria", Map.of(), comum).expectStatus().isForbidden();
    }

    @Test
    void administradorListaDoMaisNovoEGeraASenha() {
        criarConta(restClient, "joao@exemplo.com");
        pedir("maria@exemplo.com");
        jdbc.update("UPDATE pedido_recuperacao SET pedido_em = now() - interval '1 hour'");
        pedir("joao@exemplo.com");
        String admin = tokenAdmin();

        String lista = restClient.get().uri(ADMIN).header("Authorization", "Bearer " + admin).exchange()
            .expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody();
        List<String> emails = JsonPath.read(lista, "$.dados[*].email");
        assertEquals(List.of("joao@exemplo.com", "maria@exemplo.com"), emails);
        Integer idMaria = JsonPath.read(lista, "$.dados[1].id");

        String gerada = post(restClient, ADMIN + "/" + idMaria + "/senha-temporaria", Map.of(), admin)
            .expectStatus().isCreated().expectBody(String.class).returnResult().getResponseBody();
        String senha = JsonPath.read(gerada, "$.dados.senhaTemporaria");
        assertTrue(senha.matches("^[A-Z2-9]{4}-[A-Z2-9]{4}$"));
        assertEquals("maria@exemplo.com", JsonPath.read(gerada, "$.dados.email"));
        assertEquals("atendido", jdbc.queryForObject(
            "SELECT status FROM pedido_recuperacao WHERE id = ?", String.class, idMaria));
        assertFalse(jdbc.queryForObject(
            "SELECT COUNT(*) > 0 FROM senha_temporaria WHERE senha_hash = ?", Boolean.class, senha));

        post(restClient, ADMIN + "/" + idMaria + "/senha-temporaria", Map.of(), admin)
            .expectStatus().isEqualTo(422).expectBody().jsonPath("$.mensagem").isEqualTo("Pedido já atendido.");
    }

    @Test
    void pedidoInexistenteDa404() {
        post(restClient, ADMIN + "/999/senha-temporaria", Map.of(), tokenAdmin())
            .expectStatus().isNotFound().expectBody().jsonPath("$.mensagem").isEqualTo("Pedido não encontrado.");
    }
}
