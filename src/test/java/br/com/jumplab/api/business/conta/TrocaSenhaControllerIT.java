package br.com.jumplab.api.business.conta;

import br.com.jumplab.api.core.AbstractIntegrationTest;
import br.com.jumplab.api.core.seguranca.RotaProtegidaDeTesteController;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static br.com.jumplab.api.core.ContasDeTeste.criarConta;
import static br.com.jumplab.api.core.ContasDeTeste.entrar;
import static br.com.jumplab.api.core.ContasDeTeste.post;
import static br.com.jumplab.api.core.ContasDeTeste.tokenAcesso;
import static br.com.jumplab.api.core.ContasDeTeste.tokenRenovacao;
import static br.com.jumplab.api.core.ContasDeTeste.tornarAdmin;
import static br.com.jumplab.api.core.ContasDeTeste.SENHA;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** Entrar com a senha temporária, sessão com troca pendente e senha nova (spec 016, US3). */
class TrocaSenhaControllerIT extends AbstractIntegrationTest {

    private static final String SENHA_NOVA = "/api/conta/senha";

    private String temporaria;

    @BeforeEach
    void preparar() {
        jdbc.update("TRUNCATE senha_temporaria, pedido_recuperacao, token_renovacao, usuario RESTART IDENTITY CASCADE");
        criarConta(restClient, "maria@exemplo.com");
        criarConta(restClient, "admin@exemplo.com");
        tornarAdmin(jdbc, "admin@exemplo.com");
        post(restClient, "/api/recuperacao/pedidos", Map.of("email", "maria@exemplo.com")).expectStatus().isAccepted();
        String admin = tokenAcesso(entrar(restClient, "admin@exemplo.com", SENHA));
        Long pedido = jdbc.queryForObject("SELECT id FROM pedido_recuperacao", Long.class);
        String gerada = post(restClient, "/api/admin/pedidos/" + pedido + "/senha-temporaria", Map.of(), admin)
            .expectStatus().isCreated().expectBody(String.class).returnResult().getResponseBody();
        temporaria = JsonPath.read(gerada, "$.dados.senhaTemporaria");
    }

    private RestTestClient.ResponseSpec entrarCom(String senha) {
        return post(restClient, "/api/auth/entrar", Map.of("email", "maria@exemplo.com", "senha", senha));
    }

    @Test
    void temporariaEntraComTrocaObrigatoriaUmaVezSo() {
        String sessao = entrar(restClient, "maria@exemplo.com", temporaria.toLowerCase());
        assertEquals(true, JsonPath.read(sessao, "$.dados.trocaSenhaObrigatoria"));

        entrarCom(temporaria).expectStatus().isUnauthorized();
    }

    @Test
    void senhaAntigaContinuaValendoSemTroca() {
        String sessao = entrar(restClient, "maria@exemplo.com", SENHA);
        assertEquals(false, JsonPath.read(sessao, "$.dados.trocaSenhaObrigatoria"));
    }

    @Test
    void temporariaVencidaDa401ComOMesmoCorpoDeSenhaErrada() {
        jdbc.update("UPDATE senha_temporaria SET expira_em = now() - interval '1 minute'");
        String vencida = entrarCom(temporaria).expectStatus().isUnauthorized()
            .expectBody(String.class).returnResult().getResponseBody();
        String errada = entrarCom("errada123").expectStatus().isUnauthorized()
            .expectBody(String.class).returnResult().getResponseBody();
        assertEquals(errada, vencida);
    }

    @Test
    void duasEntradasSimultaneasComAMesmaTemporariaSoUmaPassa() {
        var a = CompletableFuture.supplyAsync(() -> statusDoLogin(temporaria));
        var b = CompletableFuture.supplyAsync(() -> statusDoLogin(temporaria));
        int sucessos = (a.join() == 200 ? 1 : 0) + (b.join() == 200 ? 1 : 0);
        assertEquals(1, sucessos);
    }

    private int statusDoLogin(String senha) {
        return post(restClient, "/api/auth/entrar", Map.of("email", "maria@exemplo.com", "senha", senha))
            .returnResult(String.class).getStatus().value();
    }

    @Test
    void senhaNovaComTokenPendenteEntraDepoisComASenhaNova() {
        String pendente = entrar(restClient, "maria@exemplo.com", temporaria);

        String nova = post(restClient, SENHA_NOVA, Map.of("senhaNova", "novaSenha123"), tokenAcesso(pendente))
            .expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody();
        assertEquals("Senha nova salva. A senha temporária deixou de valer.", JsonPath.read(nova, "$.mensagem"));
        assertEquals(false, JsonPath.read(nova, "$.dados.trocaSenhaObrigatoria"));

        entrar(restClient, "maria@exemplo.com", "novaSenha123");
        entrarCom(SENHA).expectStatus().isUnauthorized();
        post(restClient, "/api/auth/renovar", Map.of("tokenRenovacao", tokenRenovacao(pendente)))
            .expectStatus().isUnauthorized();
    }

    @Test
    void senhaNovaCurtaDa400() {
        String pendente = tokenAcesso(entrar(restClient, "maria@exemplo.com", temporaria));
        post(restClient, SENHA_NOVA, Map.of("senhaNova", "1234567"), pendente).expectStatus().isBadRequest();
    }

    @Test
    void senhaNovaComTokenNormal403ESemToken401() {
        String normal = tokenAcesso(entrar(restClient, "maria@exemplo.com", SENHA));
        post(restClient, SENHA_NOVA, Map.of("senhaNova", "novaSenha123"), normal).expectStatus().isForbidden()
            .expectBody().jsonPath("$.mensagem").isEqualTo("Acesso negado.");
        post(restClient, SENHA_NOVA, Map.of("senhaNova", "novaSenha123")).expectStatus().isUnauthorized();
    }

    @Test
    void renovarUmaSessaoPendenteMantemATroca() {
        String pendente = entrar(restClient, "maria@exemplo.com", temporaria);
        String renovada = post(restClient, "/api/auth/renovar", Map.of("tokenRenovacao", tokenRenovacao(pendente)))
            .expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody();
        assertEquals(true, JsonPath.read(renovada, "$.dados.trocaSenhaObrigatoria"));
    }

    // filtro da troca pendente (R2)

    @Test
    void sessaoPendenteNaoUsaOutrasRotasProtegidas() {
        String pendente = entrar(restClient, "maria@exemplo.com", temporaria);
        String token = tokenAcesso(pendente);

        restClient.get().uri(RotaProtegidaDeTesteController.CAMINHO).header("Authorization", "Bearer " + token)
            .exchange().expectStatus().isForbidden()
            .expectBody().jsonPath("$.mensagem").isEqualTo("Crie sua senha nova para continuar.");
        restClient.get().uri("/api/admin/pedidos").header("Authorization", "Bearer " + token)
            .exchange().expectStatus().isForbidden();
        post(restClient, "/api/sync/enviar", Map.of("registros", java.util.List.of()), token)
            .expectStatus().isForbidden()
            .expectBody().jsonPath("$.mensagem").isEqualTo("Crie sua senha nova para continuar.");
        restClient.get().uri("/api/sync/receber?desde=0&limite=10").header("Authorization", "Bearer " + token)
            .exchange().expectStatus().isForbidden();
        post(restClient, "/api/auth/sair", Map.of("tokenRenovacao", tokenRenovacao(pendente)))
            .expectStatus().isNoContent();
    }

    @Test
    void administradorComTrocaPendenteTambemFicaBloqueado() {
        post(restClient, "/api/recuperacao/pedidos", Map.of("email", "admin@exemplo.com")).expectStatus().isAccepted();
        String admin = tokenAcesso(entrar(restClient, "admin@exemplo.com", SENHA));
        Long pedido = jdbc.queryForObject(
            "SELECT p.id FROM pedido_recuperacao p JOIN usuario u ON u.id = p.usuario_id WHERE u.email = ?",
            Long.class, "admin@exemplo.com");
        String gerada = post(restClient, "/api/admin/pedidos/" + pedido + "/senha-temporaria", Map.of(), admin)
            .expectBody(String.class).returnResult().getResponseBody();
        String temporariaAdmin = JsonPath.read(gerada, "$.dados.senhaTemporaria");
        String pendente = tokenAcesso(entrar(restClient, "admin@exemplo.com", temporariaAdmin));

        restClient.get().uri("/api/admin/pedidos").header("Authorization", "Bearer " + pendente)
            .exchange().expectStatus().isForbidden()
            .expectBody().jsonPath("$.mensagem").isEqualTo("Crie sua senha nova para continuar.");
    }
}
