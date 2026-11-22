package br.com.jumplab.api.business.conta;

import br.com.jumplab.api.core.AbstractIntegrationTest;
import br.com.jumplab.api.core.seguranca.JwtService;
import br.com.jumplab.api.core.seguranca.PropriedadesJwt;
import br.com.jumplab.api.core.seguranca.RotaProtegidaDeTesteController;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AutenticacaoControllerIT extends AbstractIntegrationTest {

    private static final String CADASTRO = "/api/auth/cadastro";
    private static final String ENTRAR = "/api/auth/entrar";
    private static final String RENOVAR = "/api/auth/renovar";
    private static final String SAIR = "/api/auth/sair";

    @Autowired
    private JwtService jwtService;

    @BeforeEach
    void limpar() {
        jdbc.update("TRUNCATE token_renovacao, usuario RESTART IDENTITY CASCADE");
    }

    // cadastro

    @Test
    void cadastroCriaAContaEOTokenFuncionaNumaRotaProtegida() {
        String corpo = post(CADASTRO, anaCadastro()).expectStatus().isCreated()
            .expectBody(String.class).returnResult().getResponseBody();

        assertEquals("Conta criada.", JsonPath.read(corpo, "$.mensagem"));
        assertEquals("ana@exemplo.com", JsonPath.read(corpo, "$.dados.conta.email"));
        assertEquals(900, (int) JsonPath.read(corpo, "$.dados.expiraEmSegundos"));
        assertFalse(corpo.contains("senha"), "a resposta nunca leva senha nem hash");
        assertFalse(corpo.contains("$2a$"), "a resposta nunca leva hash BCrypt");

        rotaProtegida(JsonPath.read(corpo, "$.dados.tokenAcesso")).expectStatus().isOk()
            .expectBody().jsonPath("$.dados").isEqualTo("ana@exemplo.com");
    }

    @Test
    void cadastroComCampoInvalidoDa400ComAMensagemDoCampo() {
        post(CADASTRO, Map.of("nome", "A", "email", "ana@exemplo.com", "senha", "segredo123"))
            .expectStatus().isBadRequest().expectBody().jsonPath("$.mensagem").isEqualTo("Informe seu nome.");
        post(CADASTRO, Map.of("nome", "Ana", "email", "ana.exemplo.com", "senha", "segredo123"))
            .expectStatus().isBadRequest().expectBody().jsonPath("$.mensagem").isEqualTo("Informe um e-mail válido.");
        post(CADASTRO, Map.of("nome", "Ana", "email", "ana@exemplo.com", "senha", "1234567"))
            .expectStatus().isBadRequest().expectBody().jsonPath("$.mensagem")
            .isEqualTo("A senha precisa ter pelo menos 8 caracteres.");
    }

    @Test
    void cadastroComEmailRepetidoEmOutraCaixaDa422() {
        post(CADASTRO, anaCadastro()).expectStatus().isCreated();

        post(CADASTRO, Map.of("nome", "Outra Ana", "email", " ANA@Exemplo.com ", "senha", "segredo456"))
            .expectStatus().isEqualTo(422)
            .expectBody().jsonPath("$.mensagem").isEqualTo("Já existe conta com este e-mail.")
            .jsonPath("$.sucesso").isEqualTo(false);
    }

    @Test
    void jsonMalformadoDa400() {
        restClient.post().uri(CADASTRO).contentType(MediaType.APPLICATION_JSON).body("{ nao e json")
            .exchange().expectStatus().isBadRequest()
            .expectBody().jsonPath("$.mensagem").isEqualTo("Requisição inválida.");
    }

    // entrar

    @Test
    void entrarComCredenciaisCertas() {
        post(CADASTRO, anaCadastro()).expectStatus().isCreated();

        post(ENTRAR, Map.of("email", "ana@exemplo.com", "senha", "segredo123")).expectStatus().isOk()
            .expectBody().jsonPath("$.dados.conta.nome").isEqualTo("Ana Souza");
    }

    @Test
    void entrarComEmailEmMaiusculasEEspacos() {
        post(CADASTRO, anaCadastro()).expectStatus().isCreated();

        post(ENTRAR, Map.of("email", "  ANA@EXEMPLO.COM ", "senha", "segredo123")).expectStatus().isOk();
    }

    @Test
    void senhaErradaEEmailInexistenteTemCorpoIdentico() {
        post(CADASTRO, anaCadastro()).expectStatus().isCreated();

        String senhaErrada = post(ENTRAR, Map.of("email", "ana@exemplo.com", "senha", "errada123"))
            .expectStatus().isUnauthorized().expectBody(String.class).returnResult().getResponseBody();
        String semConta = post(ENTRAR, Map.of("email", "ninguem@exemplo.com", "senha", "segredo123"))
            .expectStatus().isUnauthorized().expectBody(String.class).returnResult().getResponseBody();

        assertEquals(senhaErrada, semConta);
        assertEquals("E-mail ou senha inválidos.", JsonPath.read(senhaErrada, "$.mensagem"));
    }

    @Test
    void entrarComCorpoIncompletoDa401() {
        post(ENTRAR, Map.of("email", "ana@exemplo.com")).expectStatus().isUnauthorized();
    }

    // renovar e sair

    @Test
    void renovarDevolveNovoParEOTokenAntigoPassaADar401() {
        String antigo = tokenRenovacaoDaAna();

        String corpo = post(RENOVAR, Map.of("tokenRenovacao", antigo)).expectStatus().isOk()
            .expectBody(String.class).returnResult().getResponseBody();
        String novo = JsonPath.read(corpo, "$.dados.tokenRenovacao");

        post(RENOVAR, Map.of("tokenRenovacao", antigo)).expectStatus().isUnauthorized()
            .expectBody().jsonPath("$.mensagem").isEqualTo("Sessão inválida ou expirada.");
        post(RENOVAR, Map.of("tokenRenovacao", novo)).expectStatus().isOk();
    }

    @Test
    void tokenDeRenovacaoVencidoDa401() {
        String token = tokenRenovacaoDaAna();
        jdbc.update("UPDATE token_renovacao SET expira_em = now() - interval '1 minute'");

        post(RENOVAR, Map.of("tokenRenovacao", token)).expectStatus().isUnauthorized();
    }

    @Test
    void tokenDeRenovacaoInexistenteDa401() {
        post(RENOVAR, Map.of("tokenRenovacao", "inventado")).expectStatus().isUnauthorized();
    }

    @Test
    void sairRevogaEOTokenPassaADar401() {
        String token = tokenRenovacaoDaAna();

        post(SAIR, Map.of("tokenRenovacao", token)).expectStatus().isNoContent();
        post(SAIR, Map.of("tokenRenovacao", token)).expectStatus().isNoContent();

        post(RENOVAR, Map.of("tokenRenovacao", token)).expectStatus().isUnauthorized();
    }

    // rotas protegidas

    @Test
    void rotaProtegidaSemTokenDa401() {
        restClient.get().uri(RotaProtegidaDeTesteController.CAMINHO).exchange()
            .expectStatus().isUnauthorized()
            .expectBody().jsonPath("$.mensagem").isEqualTo("Sessão inválida ou expirada.")
            .jsonPath("$.sucesso").isEqualTo(false);
    }

    @Test
    void rotaProtegidaComTokenVencidoDa401() {
        String vencido = jwtService.emitir(1L, "ana@exemplo.com", false, Instant.now().minus(Duration.ofHours(1)));

        rotaProtegida(vencido).expectStatus().isUnauthorized()
            .expectBody().jsonPath("$.mensagem").isEqualTo("Sessão inválida ou expirada.");
    }

    @Test
    void rotaProtegidaComTokenDeOutroSegredoDa401() {
        JwtService outro = new JwtService(new PropriedadesJwt("outro-segredo-de-teste-com-32-bytes-ou-mais", 15, 30));

        rotaProtegida(outro.emitir(1L, "ana@exemplo.com", false)).expectStatus().isUnauthorized();
    }

    @Test
    void rotaProtegidaComTokenMalformadoDa401() {
        rotaProtegida("nao-e-um-jwt").expectStatus().isUnauthorized();
    }

    private String tokenRenovacaoDaAna() {
        String corpo = post(CADASTRO, anaCadastro()).expectStatus().isCreated()
            .expectBody(String.class).returnResult().getResponseBody();
        return JsonPath.read(corpo, "$.dados.tokenRenovacao");
    }

    private RestTestClient.ResponseSpec post(String caminho, Map<String, String> corpo) {
        return restClient.post().uri(caminho).contentType(MediaType.APPLICATION_JSON).body(corpo).exchange();
    }

    private RestTestClient.ResponseSpec rotaProtegida(String tokenAcesso) {
        return restClient.get().uri(RotaProtegidaDeTesteController.CAMINHO)
            .header("Authorization", "Bearer " + tokenAcesso).exchange();
    }

    private static Map<String, String> anaCadastro() {
        return Map.of("nome", "Ana Souza", "email", "ana@exemplo.com", "senha", "segredo123");
    }
}
