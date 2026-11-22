package br.com.jumplab.api.core;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.Map;

/** Atalhos dos testes de integração: criar conta, virar administrador e entrar. */
public final class ContasDeTeste {

    public static final String SENHA = "segredo123";

    private ContasDeTeste() {}

    public static RestTestClient.ResponseSpec post(RestTestClient cliente, String caminho, Object corpo) {
        return cliente.post().uri(caminho).contentType(MediaType.APPLICATION_JSON).body(corpo).exchange();
    }

    public static RestTestClient.ResponseSpec post(RestTestClient cliente, String caminho, Object corpo, String token) {
        return cliente.post().uri(caminho).contentType(MediaType.APPLICATION_JSON)
            .header("Authorization", "Bearer " + token).body(corpo).exchange();
    }

    public static void criarConta(RestTestClient cliente, String email) {
        post(cliente, "/api/auth/cadastro", Map.of("nome", "Nome", "email", email, "senha", SENHA))
            .expectStatus().isCreated();
    }

    public static void tornarAdmin(JdbcTemplate jdbc, String email) {
        jdbc.update("UPDATE usuario SET admin = true WHERE email = ?", email);
    }

    /** Corpo da resposta do login (SessaoDTO no envelope). */
    public static String entrar(RestTestClient cliente, String email, String senha) {
        return post(cliente, "/api/auth/entrar", Map.of("email", email, "senha", senha))
            .expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody();
    }

    public static String tokenAcesso(String corpoSessao) {
        return JsonPath.read(corpoSessao, "$.dados.tokenAcesso");
    }

    public static String tokenRenovacao(String corpoSessao) {
        return JsonPath.read(corpoSessao, "$.dados.tokenRenovacao");
    }
}
