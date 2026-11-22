package br.com.jumplab.api.business.sincronizacao;

import br.com.jumplab.api.core.exceptions.FieldValidationException;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static br.com.jumplab.api.business.sincronizacao.SincronizacaoValidationTest.AVALIACAO;
import static br.com.jumplab.api.business.sincronizacao.SincronizacaoValidationTest.PACIENTE;
import static br.com.jumplab.api.business.sincronizacao.SincronizacaoValidationTest.avaliacao;
import static br.com.jumplab.api.business.sincronizacao.SincronizacaoValidationTest.paciente;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SincronizacaoServiceTest {

    private static final long USUARIO = 7L;

    private final IPacienteSyncRepository pacientes = mock(IPacienteSyncRepository.class);
    private final IAvaliacaoSyncRepository avaliacoes = mock(IAvaliacaoSyncRepository.class);
    private final ISincronizacaoValidation validacao = mock(ISincronizacaoValidation.class);
    private final SincronizacaoService servico =
        new SincronizacaoService(pacientes, avaliacoes, validacao, JsonMapper.builder().build());

    private void tudoValido() {
        when(validacao.motivoDaRecusa(anyString(), any())).thenReturn(Optional.empty());
        when(pacientes.pacienteDaConta(any(), anyLong())).thenReturn(true);
        when(pacientes.gravarSeMaisRecente(anyLong(), any(), any(), any(), anyString())).thenReturn(true);
        when(avaliacoes.gravarSeMaisRecente(anyLong(), any(), any(), any(), anyString())).thenReturn(true);
    }

    @Test
    void validaOLoteAntesDeGravar() {
        LoteEnvioDTO lote = new LoteEnvioDTO(List.of(paciente()), List.of());
        doThrow(new FieldValidationException("lote", "limite")).when(validacao).validarLote(lote);
        assertThrows(FieldValidationException.class, () -> servico.enviar(USUARIO, lote));
        verify(pacientes, never()).gravarSeMaisRecente(anyLong(), any(), any(), any(), anyString());
    }

    @Test
    void gravaPacientesAntesDasAvaliacoesComOUsuarioDoParametro() {
        tudoValido();
        ResultadoEnvioDTO r = servico.enviar(USUARIO, new LoteEnvioDTO(List.of(paciente()), List.of(avaliacao())));

        InOrder ordem = inOrder(pacientes, avaliacoes);
        ordem.verify(pacientes).gravarSeMaisRecente(eq(USUARIO), eq(UUID.fromString(PACIENTE)), any(),
            eq(Instant.parse("2026-11-22T13:58:00.000Z")), anyString());
        ordem.verify(avaliacoes).gravarSeMaisRecente(eq(USUARIO), eq(UUID.fromString(AVALIACAO)),
            eq(UUID.fromString(PACIENTE)), any(), anyString());
        assertEquals(List.of("aceito", "aceito"), r.resultados().stream().map(ResultadoRegistroDTO::situacao).toList());
    }

    @Test
    void registroInvalidoViraRecusadoSemPararOLote() {
        tudoValido();
        RegistroSyncDTO ruim = new RegistroSyncDTO("x", null, null, null);
        when(validacao.motivoDaRecusa("paciente", ruim)).thenReturn(Optional.of("Identificador inválido."));

        ResultadoEnvioDTO r = servico.enviar(USUARIO, new LoteEnvioDTO(List.of(ruim, paciente()), List.of()));

        assertEquals(new ResultadoRegistroDTO("paciente", "x", "recusado", "Identificador inválido."), r.resultados().get(0));
        assertEquals("aceito", r.resultados().get(1).situacao());
    }

    @Test
    void avaliacaoDePacienteForaDaContaERecusada() {
        tudoValido();
        when(pacientes.pacienteDaConta(UUID.fromString(PACIENTE), USUARIO)).thenReturn(false);

        ResultadoEnvioDTO r = servico.enviar(USUARIO, new LoteEnvioDTO(List.of(), List.of(avaliacao())));

        assertEquals("Paciente não encontrado na conta.", r.resultados().get(0).motivo());
        verify(avaliacoes, never()).gravarSeMaisRecente(anyLong(), any(), any(), any(), anyString());
    }

    @Test
    void naoGravouIgnoradoSeEDaContaERecusadoSeEDeOutra() {
        tudoValido();
        when(pacientes.gravarSeMaisRecente(anyLong(), any(), any(), any(), anyString())).thenReturn(false);
        when(pacientes.pertenceAOutroUsuario(any(), eq(USUARIO))).thenReturn(false, true);

        ResultadoEnvioDTO r = servico.enviar(USUARIO, new LoteEnvioDTO(List.of(paciente(), paciente()), List.of()));

        assertEquals("ignorado", r.resultados().get(0).situacao());
        assertEquals(new ResultadoRegistroDTO("paciente", PACIENTE, "recusado", "Registro não pertence à conta."),
            r.resultados().get(1));
    }

    @Test
    void receberJuntaAsDuasTabelasEmOrdemDeVersaoComPaginacao() {
        RegistroSyncModel p1 = new RegistroSyncModel(UUID.fromString(PACIENTE), null,
            Instant.parse("2026-11-22T13:58:00Z"), "{\"id\":\"" + PACIENTE + "\"}", 3);
        RegistroSyncModel a1 = new RegistroSyncModel(UUID.fromString(AVALIACAO), UUID.fromString(PACIENTE),
            Instant.parse("2026-11-22T13:59:00.120Z"), "{\"avaliacao\":{}}", 5);
        when(pacientes.listarDesde(USUARIO, 2, 2)).thenReturn(List.of(p1));
        when(avaliacoes.listarDesde(USUARIO, 2, 2)).thenReturn(List.of(a1));

        PaginaRecebimentoDTO pagina = servico.receber(USUARIO, 2, 1);

        assertEquals(1, pagina.pacientes().size());
        assertTrue(pagina.avaliacoes().isEmpty());
        assertEquals(3, pagina.cursor());
        assertTrue(pagina.temMais());
        assertEquals("2026-11-22T13:58:00.000Z", pagina.pacientes().get(0).atualizadoEm());
    }

    @Test
    void paginaVaziaDevolveOProprioCursor() {
        PaginaRecebimentoDTO pagina = servico.receber(USUARIO, 9, 200);
        assertEquals(9, pagina.cursor());
        assertFalse(pagina.temMais());
    }

    @Test
    void parametrosInvalidos() {
        assertThrows(FieldValidationException.class, () -> servico.receber(USUARIO, -1, 10));
        assertThrows(FieldValidationException.class, () -> servico.receber(USUARIO, 0, 0));
        assertThrows(FieldValidationException.class, () -> servico.receber(USUARIO, 0, 201));
    }
}
