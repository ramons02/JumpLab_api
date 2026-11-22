package br.com.jumplab.api.business.sincronizacao;

import br.com.jumplab.api.core.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SincronizacaoRepositoryIT extends AbstractIntegrationTest {

    private static final UUID PACIENTE = UUID.fromString("6f1c0a52-8a9e-4a4e-9a51-0f5f3b0b8c11");
    private static final Instant T1 = Instant.parse("2026-11-22T13:58:00.000Z");
    private static final Instant T2 = Instant.parse("2026-11-22T14:03:10.120Z");
    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Autowired
    private IPacienteSyncRepository pacientes;

    private long ana;
    private long bruno;

    @BeforeEach
    void preparar() {
        jdbc.update("TRUNCATE avaliacao_sync, paciente_sync, token_renovacao, usuario RESTART IDENTITY CASCADE");
        ana = criarUsuario("ana@exemplo.com");
        bruno = criarUsuario("bruno@exemplo.com");
    }

    private long criarUsuario(String email) {
        return jdbc.queryForObject(
            "INSERT INTO usuario (nome, email, senha_hash, admin) VALUES ('X', ?, 'h', false) RETURNING id",
            Long.class, email);
    }

    private String conteudo(String nome) {
        return "{\"id\":\"" + PACIENTE + "\",\"nome\":\"" + nome + "\",\"altura\":30.656,\"quadro\":1000}";
    }

    private long versao() {
        return jdbc.queryForObject("SELECT versao FROM paciente_sync WHERE id = ?", Long.class, PACIENTE);
    }

    @Test
    void insereEAtualizaSoQuandoAAlteracaoEMaisRecente() {
        assertTrue(pacientes.gravarSeMaisRecente(ana, PACIENTE, null, T1, conteudo("Ana")));
        long primeira = versao();

        assertTrue(pacientes.gravarSeMaisRecente(ana, PACIENTE, null, T2, conteudo("Ana Souza")));
        assertNotEquals(primeira, versao());

        long depois = versao();
        assertFalse(pacientes.gravarSeMaisRecente(ana, PACIENTE, null, T2, conteudo("igual")));
        assertFalse(pacientes.gravarSeMaisRecente(ana, PACIENTE, null, T1, conteudo("velho")));
        assertEquals(depois, versao());
        assertTrue(jdbc.queryForObject("SELECT conteudo::text FROM paciente_sync", String.class).contains("Ana Souza"));
    }

    @Test
    void outraContaNaoSobrescreveEPertenceAOutroUsuarioAcusa() {
        pacientes.gravarSeMaisRecente(ana, PACIENTE, null, T1, conteudo("Ana"));

        assertFalse(pacientes.gravarSeMaisRecente(bruno, PACIENTE, null, T2, conteudo("Invasor")));
        assertTrue(pacientes.pertenceAOutroUsuario(PACIENTE, bruno));
        assertFalse(pacientes.pertenceAOutroUsuario(PACIENTE, ana));
        assertTrue(jdbc.queryForObject("SELECT conteudo::text FROM paciente_sync", String.class).contains("\"Ana\""));
    }

    @Test
    void conteudoVoltaEquivalenteComDecimaisEInteiros() {
        pacientes.gravarSeMaisRecente(ana, PACIENTE, null, T1, conteudo("Ana"));

        List<RegistroSyncModel> lidos = pacientes.listarDesde(ana, 0, 10);

        assertEquals(1, lidos.size());
        assertEquals(JSON.readTree(conteudo("Ana")), JSON.readTree(lidos.get(0).conteudo()));
        assertEquals(T1, lidos.get(0).atualizadoEm());
        assertTrue(pacientes.listarDesde(bruno, 0, 10).isEmpty());
        assertTrue(pacientes.pacienteDaConta(PACIENTE, ana));
        assertFalse(pacientes.pacienteDaConta(PACIENTE, bruno));
    }
}
