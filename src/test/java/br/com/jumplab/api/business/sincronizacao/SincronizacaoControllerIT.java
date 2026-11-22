package br.com.jumplab.api.business.sincronizacao;

import br.com.jumplab.api.core.AbstractIntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SincronizacaoControllerIT extends AbstractIntegrationTest {

    private static final String ENVIAR = "/api/sync/enviar";
    private static final String RECEBER = "/api/sync/receber";
    private static final String PACIENTE = "6f1c0a52-8a9e-4a4e-9a51-0f5f3b0b8c11";

    private String tokenAna;
    private String tokenBruno;

    @BeforeEach
    void preparar() {
        jdbc.update("TRUNCATE avaliacao_sync, paciente_sync, token_renovacao, usuario RESTART IDENTITY CASCADE");
        tokenAna = cadastrar("Ana Souza", "ana@exemplo.com");
        tokenBruno = cadastrar("Bruno Lima", "bruno@exemplo.com");
    }

    private String cadastrar(String nome, String email) {
        String corpo = restClient.post().uri("/api/auth/cadastro").contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("nome", nome, "email", email, "senha", "segredo123"))
            .exchange().expectStatus().isCreated().expectBody(String.class).returnResult().getResponseBody();
        return JsonPath.read(corpo, "$.dados.tokenAcesso");
    }

    private static Map<String, Object> paciente(String id, String atualizadoEm, String nome) {
        return Map.of("id", id, "atualizadoEm", atualizadoEm,
            "conteudo", Map.of("id", id, "nome", nome, "perfil", "pos_lca", "atualizado_em", atualizadoEm));
    }

    private static Map<String, Object> avaliacao(String id, String pacienteId, String atualizadoEm) {
        return Map.of("id", id, "pacienteId", pacienteId, "atualizadoEm", atualizadoEm,
            "conteudo", Map.of(
                "avaliacao", Map.of("id", id, "paciente_id", pacienteId, "resultado_json", "{\"versao\":1}"),
                "tentativas", List.of(Map.of("numero", 1, "fps", 240.0, "quadro_decolagem", 1000)),
                "repeticoes", List.of()));
    }

    private RestTestClient.ResponseSpec enviar(String token, Object lote) {
        RestTestClient.RequestBodySpec pedido = restClient.post().uri(ENVIAR).contentType(MediaType.APPLICATION_JSON);
        if (token != null) pedido = pedido.header("Authorization", "Bearer " + token);
        return pedido.body(lote).exchange();
    }

    private String receber(String token, long desde, int limite) {
        return restClient.get().uri(RECEBER + "?desde=" + desde + "&limite=" + limite)
            .header("Authorization", "Bearer " + token).exchange().expectStatus().isOk()
            .expectBody(String.class).returnResult().getResponseBody();
    }

    private static List<String> situacoes(String corpo) {
        return JsonPath.read(corpo, "$.dados.resultados[*].situacao");
    }

    private Map<String, Object> loteDaAna() {
        List<Object> avaliacoes = new ArrayList<>();
        for (int i = 0; i < 3; i++) avaliacoes.add(avaliacao(UUID.randomUUID().toString(), PACIENTE, "2026-11-22T14:1" + i + ":00.000Z"));
        return Map.of("pacientes", List.of(paciente(PACIENTE, "2026-11-22T14:03:10.120Z", "Ana Souza")),
            "avaliacoes", avaliacoes);
    }

    @Test
    void loteDeUmPacienteE3AvaliacoesEAceitoEReenvioEIgnoradoSemDuplicar() {
        Map<String, Object> lote = loteDaAna();
        String primeiro = enviar(tokenAna, lote).expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody();
        assertEquals(List.of("aceito", "aceito", "aceito", "aceito"), situacoes(primeiro));

        String segundo = enviar(tokenAna, lote).expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody();
        assertEquals(List.of("ignorado", "ignorado", "ignorado", "ignorado"), situacoes(segundo));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM paciente_sync", Integer.class));
        assertEquals(3, jdbc.queryForObject("SELECT COUNT(*) FROM avaliacao_sync", Integer.class));
    }

    @Test
    void versaoMaisVelhaEIgnorada() {
        enviar(tokenAna, Map.of("pacientes", List.of(paciente(PACIENTE, "2026-11-22T14:03:10.120Z", "Nova")))).expectStatus().isOk();
        String corpo = enviar(tokenAna, Map.of("pacientes", List.of(paciente(PACIENTE, "2026-11-22T14:00:00.000Z", "Velha"))))
            .expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody();
        assertEquals(List.of("ignorado"), situacoes(corpo));
    }

    @Test
    void loteCom101RegistrosDa400() {
        List<Object> pacientes = new ArrayList<>();
        for (int i = 0; i < 101; i++) {
            String id = UUID.randomUUID().toString();
            pacientes.add(paciente(id, "2026-11-22T14:00:00.000Z", "P" + i));
        }
        enviar(tokenAna, Map.of("pacientes", pacientes)).expectStatus().isBadRequest()
            .expectBody().jsonPath("$.mensagem").isEqualTo("O lote passa do limite de 100 registros.");
    }

    @Test
    void corpoAcimaDe2MbDa413() {
        String grande = "x".repeat(2 * 1024 * 1024 + 10);
        enviar(tokenAna, Map.of("pacientes", List.of(paciente(PACIENTE, "2026-11-22T14:00:00.000Z", grande))))
            .expectStatus().isEqualTo(413).expectBody().jsonPath("$.mensagem").isEqualTo("Requisição grande demais.");
    }

    @Test
    void avaliacaoComChaveDeVideoERecusada() {
        Map<String, Object> comVideo = Map.of("id", UUID.randomUUID().toString(), "pacienteId", PACIENTE,
            "atualizadoEm", "2026-11-22T14:10:00.000Z",
            "conteudo", Map.of("avaliacao", Map.of("id", "x"), "video", "file:///salto.mp4"));
        String id = (String) comVideo.get("id");
        Map<String, Object> corrigido = Map.of("id", id, "pacienteId", PACIENTE, "atualizadoEm", "2026-11-22T14:10:00.000Z",
            "conteudo", Map.of("avaliacao", Map.of("id", id), "tentativas", List.of(Map.of("video", "file:///salto.mp4"))));
        String corpo = enviar(tokenAna, Map.of("pacientes", List.of(paciente(PACIENTE, "2026-11-22T14:00:00.000Z", "Ana")),
            "avaliacoes", List.of(corrigido))).expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody();
        assertEquals(List.of("aceito", "recusado"), situacoes(corpo));
        assertEquals("Vídeo não é sincronizado.", JsonPath.read(corpo, "$.dados.resultados[1].motivo"));
    }

    @Test
    void receberDesdeZeroTrazTudoEmOrdemComPaginacaoECursor() {
        enviar(tokenAna, loteDaAna()).expectStatus().isOk();

        String tudo = receber(tokenAna, 0, 200);
        assertEquals(1, (int) JsonPath.read(tudo, "$.dados.pacientes.length()"));
        assertEquals(3, (int) JsonPath.read(tudo, "$.dados.avaliacoes.length()"));
        assertFalse((Boolean) JsonPath.read(tudo, "$.dados.temMais"));
        assertEquals("Ana Souza", JsonPath.read(tudo, "$.dados.pacientes[0].conteudo.nome"));
        assertEquals(1000, (int) JsonPath.read(tudo, "$.dados.avaliacoes[0].conteudo.tentativas[0].quadro_decolagem"));
        assertEquals("2026-11-22T14:03:10.120Z", JsonPath.read(tudo, "$.dados.pacientes[0].atualizadoEm"));

        String primeira = receber(tokenAna, 0, 2);
        assertTrue((Boolean) JsonPath.read(primeira, "$.dados.temMais"));
        long cursor = ((Number) JsonPath.read(primeira, "$.dados.cursor")).longValue();
        String resto = receber(tokenAna, cursor, 2);
        assertEquals(2, (int) JsonPath.read(resto, "$.dados.avaliacoes.length()"));
        long fim = ((Number) JsonPath.read(resto, "$.dados.cursor")).longValue();

        String vazia = receber(tokenAna, fim, 2);
        assertEquals(fim, ((Number) JsonPath.read(vazia, "$.dados.cursor")).longValue());
        assertFalse((Boolean) JsonPath.read(vazia, "$.dados.temMais"));
    }

    @Test
    void receberComParametrosInvalidosDa400() {
        restClient.get().uri(RECEBER + "?desde=-1").header("Authorization", "Bearer " + tokenAna)
            .exchange().expectStatus().isBadRequest().expectBody().jsonPath("$.mensagem").isEqualTo("Parâmetros inválidos.");
        restClient.get().uri(RECEBER + "?limite=201").header("Authorization", "Bearer " + tokenAna)
            .exchange().expectStatus().isBadRequest();
    }

    // isolamento entre contas (US4)

    @Test
    void semTokenEnviarEReceberDao401() {
        enviar(null, loteDaAna()).expectStatus().isUnauthorized();
        restClient.get().uri(RECEBER).exchange().expectStatus().isUnauthorized();
    }

    @Test
    void outraContaNaoRecebeNemAlteraOsDadosDaPrimeira() {
        enviar(tokenAna, loteDaAna()).expectStatus().isOk();

        String doBruno = receber(tokenBruno, 0, 200);
        assertEquals(0, (int) JsonPath.read(doBruno, "$.dados.pacientes.length()"));
        assertEquals(0, (int) JsonPath.read(doBruno, "$.dados.avaliacoes.length()"));

        String invasao = enviar(tokenBruno, Map.of(
                "pacientes", List.of(paciente(PACIENTE, "2026-11-22T14:30:00.000Z", "Invasor")),
                "avaliacoes", List.of(avaliacao(UUID.randomUUID().toString(), PACIENTE, "2026-11-22T14:30:00.000Z"))))
            .expectStatus().isOk().expectBody(String.class).returnResult().getResponseBody();
        assertEquals(List.of("recusado", "recusado"), situacoes(invasao));
        assertEquals("Registro não pertence à conta.", JsonPath.read(invasao, "$.dados.resultados[0].motivo"));
        assertEquals("Paciente não encontrado na conta.", JsonPath.read(invasao, "$.dados.resultados[1].motivo"));
        assertFalse(invasao.contains("ana@exemplo.com"));
        assertTrue(jdbc.queryForObject("SELECT conteudo->>'nome' FROM paciente_sync", String.class).equals("Ana Souza"));
    }
}
