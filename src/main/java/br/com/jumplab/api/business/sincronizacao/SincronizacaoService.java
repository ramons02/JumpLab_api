package br.com.jumplab.api.business.sincronizacao;

import br.com.jumplab.api.core.exceptions.FieldValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Troca de dados com o app (HU013). O usuário vem sempre do token (parâmetro), nunca do corpo.
 * Conflito: vence a alteração mais recente (D15). Logs só com contagens, nunca o conteúdo.
 */
@Service
public class SincronizacaoService implements ISincronizacaoService {

    static final String MENSAGEM_OUTRA_CONTA = "Registro não pertence à conta.";
    static final String MENSAGEM_PACIENTE_FORA = "Paciente não encontrado na conta.";
    static final String MENSAGEM_PARAMETROS = "Parâmetros inválidos.";
    static final int LIMITE_PADRAO = 200;
    static final int LIMITE_MAXIMO = 200;

    /** Mesmo formato do app (ISO 8601 UTC com milissegundos), para as datas compararem igual. */
    static final DateTimeFormatter DATA_DO_APP =
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC);

    private static final Logger log = LoggerFactory.getLogger(SincronizacaoService.class);

    private final IPacienteSyncRepository pacientes;
    private final IAvaliacaoSyncRepository avaliacoes;
    private final ISincronizacaoValidation validacao;
    private final ObjectMapper json;

    public SincronizacaoService(
        IPacienteSyncRepository pacientes,
        IAvaliacaoSyncRepository avaliacoes,
        ISincronizacaoValidation validacao,
        ObjectMapper json) {
        this.pacientes = pacientes;
        this.avaliacoes = avaliacoes;
        this.validacao = validacao;
        this.json = json;
    }

    @Override
    @Transactional
    public ResultadoEnvioDTO enviar(long usuarioId, LoteEnvioDTO lote) {
        validacao.validarLote(lote);
        List<ResultadoRegistroDTO> resultados = new ArrayList<>();
        // Pacientes antes das avaliações: a avaliação precisa do paciente já na conta (research R7).
        for (RegistroSyncDTO registro : lote.pacientesOuVazio()) {
            resultados.add(gravar(usuarioId, ResultadoRegistroDTO.PACIENTE, registro, pacientes));
        }
        for (RegistroSyncDTO registro : lote.avaliacoesOuVazio()) {
            resultados.add(gravar(usuarioId, ResultadoRegistroDTO.AVALIACAO, registro, avaliacoes));
        }
        log.info(
            "Sincronização recebida do usuário {}: {} aceitos, {} ignorados, {} recusados",
            usuarioId,
            contar(resultados, ResultadoRegistroDTO.ACEITO),
            contar(resultados, ResultadoRegistroDTO.IGNORADO),
            contar(resultados, ResultadoRegistroDTO.RECUSADO));
        return new ResultadoEnvioDTO(resultados);
    }

    private ResultadoRegistroDTO gravar(
        long usuarioId, String tipo, RegistroSyncDTO registro, ISyncRepository repositorio) {
        Optional<String> motivo = validacao.motivoDaRecusa(tipo, registro);
        if (motivo.isPresent()) return recusado(tipo, registro, motivo.get());

        UUID id = UUID.fromString(registro.id());
        UUID pacienteId = registro.pacienteId() == null ? null : UUID.fromString(registro.pacienteId());
        if (pacienteId != null && !pacientes.pacienteDaConta(pacienteId, usuarioId)) {
            return recusado(tipo, registro, MENSAGEM_PACIENTE_FORA);
        }
        boolean gravou = repositorio.gravarSeMaisRecente(
            usuarioId, id, pacienteId, Instant.parse(registro.atualizadoEm()), registro.conteudo().toString());
        if (gravou) return new ResultadoRegistroDTO(tipo, registro.id(), ResultadoRegistroDTO.ACEITO, null);
        if (repositorio.pertenceAOutroUsuario(id, usuarioId)) {
            // Não diz a quem pertence; o registro da outra conta fica intacto.
            return recusado(tipo, registro, MENSAGEM_OUTRA_CONTA);
        }
        return new ResultadoRegistroDTO(tipo, registro.id(), ResultadoRegistroDTO.IGNORADO, null);
    }

    private static ResultadoRegistroDTO recusado(String tipo, RegistroSyncDTO registro, String motivo) {
        return new ResultadoRegistroDTO(tipo, registro.id(), ResultadoRegistroDTO.RECUSADO, motivo);
    }

    private static long contar(List<ResultadoRegistroDTO> resultados, String situacao) {
        return resultados.stream().filter(r -> situacao.equals(r.situacao())).count();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaRecebimentoDTO receber(long usuarioId, long desde, int limite) {
        if (desde < 0 || limite < 1 || limite > LIMITE_MAXIMO) {
            throw new FieldValidationException("desde", MENSAGEM_PARAMETROS);
        }
        // As duas tabelas usam a mesma sequência: junta, ordena por versão e corta no limite.
        List<Marcado> juntos = Stream.concat(
                pacientes.listarDesde(usuarioId, desde, limite + 1).stream().map(m -> new Marcado(true, m)),
                avaliacoes.listarDesde(usuarioId, desde, limite + 1).stream().map(m -> new Marcado(false, m)))
            .sorted(Comparator.comparingLong(m -> m.modelo().versao()))
            .toList();
        List<Marcado> pagina = juntos.subList(0, Math.min(limite, juntos.size()));
        long cursor = pagina.isEmpty() ? desde : pagina.get(pagina.size() - 1).modelo().versao();
        return new PaginaRecebimentoDTO(
            pagina.stream().filter(Marcado::paciente).map(m -> paraDto(m.modelo())).toList(),
            pagina.stream().filter(m -> !m.paciente()).map(m -> paraDto(m.modelo())).toList(),
            cursor,
            juntos.size() > limite);
    }

    private RegistroSyncDTO paraDto(RegistroSyncModel modelo) {
        return new RegistroSyncDTO(
            modelo.id().toString(),
            modelo.pacienteId() == null ? null : modelo.pacienteId().toString(),
            DATA_DO_APP.format(modelo.atualizadoEm()),
            json.readTree(modelo.conteudo()));
    }

    private record Marcado(boolean paciente, RegistroSyncModel modelo) {}
}
