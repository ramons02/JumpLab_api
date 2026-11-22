package br.com.jumplab.api.business.sincronizacao;

import br.com.jumplab.api.core.exceptions.FieldValidationException;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SincronizacaoValidationTest {

    static final String PACIENTE = "6f1c0a52-8a9e-4a4e-9a51-0f5f3b0b8c11";
    static final String AVALIACAO = "0b7d5f0e-3c1a-4d43-8f0e-6a2d9b7c4e21";
    private static final Instant AGORA = Instant.parse("2026-11-22T14:00:00.000Z");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final SincronizacaoValidation validacao =
        new SincronizacaoValidation(Clock.fixed(AGORA, ZoneOffset.UTC));

    static JsonNode json(String texto) {
        return JSON.readTree(texto);
    }

    static RegistroSyncDTO paciente() {
        return new RegistroSyncDTO(PACIENTE, null, "2026-11-22T13:58:00.000Z",
            json("{\"id\":\"" + PACIENTE + "\",\"nome\":\"Ana Souza\",\"perfil\":\"pos_lca\"}"));
    }

    static RegistroSyncDTO avaliacao() {
        return new RegistroSyncDTO(AVALIACAO, PACIENTE, "2026-11-22T13:59:00.000Z",
            json("{\"avaliacao\":{\"id\":\"" + AVALIACAO + "\",\"paciente_id\":\"" + PACIENTE
                + "\",\"resultado_json\":\"{}\"},\"tentativas\":[{\"quadro_decolagem\":1000,\"fps\":240.0}],"
                + "\"serieAgachamento\":null,\"repeticoes\":[]}"));
    }

    private Optional<String> motivo(String tipo, RegistroSyncDTO registro) {
        return validacao.motivoDaRecusa(tipo, registro);
    }

    @Test
    void registrosValidosDeCadaTipoPassam() {
        assertEquals(Optional.empty(), motivo("paciente", paciente()));
        assertEquals(Optional.empty(), motivo("avaliacao", avaliacao()));
    }

    @Test
    void loteComMaisDe100RegistrosERecusadoInteiro() {
        List<RegistroSyncDTO> cem = Collections.nCopies(100, paciente());
        validacao.validarLote(new LoteEnvioDTO(cem, List.of()));
        FieldValidationException erro = assertThrows(FieldValidationException.class,
            () -> validacao.validarLote(new LoteEnvioDTO(cem, List.of(avaliacao()))));
        assertEquals("O lote passa do limite de 100 registros.", erro.getMessage());
    }

    @Test
    void identificadorInvalido() {
        RegistroSyncDTO p = paciente();
        assertEquals(Optional.of("Identificador inválido."),
            motivo("paciente", new RegistroSyncDTO("nao-e-uuid", null, p.atualizadoEm(), p.conteudo())));
        RegistroSyncDTO a = avaliacao();
        assertEquals(Optional.of("Identificador inválido."),
            motivo("avaliacao", new RegistroSyncDTO(a.id(), null, a.atualizadoEm(), a.conteudo())));
    }

    @Test
    void dataAusenteOuMaisDe5MinutosNoFuturo() {
        RegistroSyncDTO p = paciente();
        assertEquals(Optional.of("Data de alteração inválida."),
            motivo("paciente", new RegistroSyncDTO(p.id(), null, null, p.conteudo())));
        assertEquals(Optional.of("Data de alteração inválida."),
            motivo("paciente", new RegistroSyncDTO(p.id(), null, "2026-11-22T14:05:01.000Z", p.conteudo())));
        assertEquals(Optional.empty(),
            motivo("paciente", new RegistroSyncDTO(p.id(), null, "2026-11-22T14:05:00.000Z", p.conteudo())));
    }

    @Test
    void conteudoQueNaoEObjetoOuComIdDiferente() {
        RegistroSyncDTO p = paciente();
        assertEquals(Optional.of("Conteúdo inválido."),
            motivo("paciente", new RegistroSyncDTO(p.id(), null, p.atualizadoEm(), json("[1,2]"))));
        assertEquals(Optional.of("Conteúdo inválido."),
            motivo("paciente", new RegistroSyncDTO(p.id(), null, p.atualizadoEm(),
                json("{\"id\":\"" + AVALIACAO + "\"}"))));
        RegistroSyncDTO a = avaliacao();
        assertEquals(Optional.of("Conteúdo inválido."),
            motivo("avaliacao", new RegistroSyncDTO(a.id(), a.pacienteId(), a.atualizadoEm(),
                json("{\"avaliacao\":{\"id\":\"" + PACIENTE + "\"}}"))));
    }

    @Test
    void chaveDeVideoEmQualquerNivelERecusada() {
        RegistroSyncDTO a = avaliacao();
        JsonNode aninhado = json("{\"avaliacao\":{\"id\":\"" + AVALIACAO + "\"},\"tentativas\":[{\"videoUri\":\"file:///x.mp4\"}]}");
        assertEquals(Optional.of("Vídeo não é sincronizado."),
            motivo("avaliacao", new RegistroSyncDTO(a.id(), a.pacienteId(), a.atualizadoEm(), aninhado)));
        RegistroSyncDTO p = paciente();
        assertEquals(Optional.of("Vídeo não é sincronizado."),
            motivo("paciente", new RegistroSyncDTO(p.id(), null, p.atualizadoEm(),
                json("{\"id\":\"" + PACIENTE + "\",\"video\":{\"uri\":\"x\"}}"))));
    }
}
