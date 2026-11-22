package br.com.jumplab.api.business.sincronizacao;

import br.com.jumplab.api.core.exceptions.FieldValidationException;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Regras do envio (research R6): o lote tem limite; cada registro é aceito ou recusado sozinho. */
@Component
public class SincronizacaoValidation implements ISincronizacaoValidation {

    static final int LIMITE_DO_LOTE = 100;
    static final Duration TOLERANCIA_FUTURO = Duration.ofMinutes(5);

    static final String MENSAGEM_LOTE_GRANDE = "O lote passa do limite de 100 registros.";
    static final String MENSAGEM_ID = "Identificador inválido.";
    static final String MENSAGEM_DATA = "Data de alteração inválida.";
    static final String MENSAGEM_CONTEUDO = "Conteúdo inválido.";
    static final String MENSAGEM_VIDEO = "Vídeo não é sincronizado.";

    private final Clock relogio;

    public SincronizacaoValidation() {
        this(Clock.systemUTC());
    }

    SincronizacaoValidation(Clock relogio) {
        this.relogio = relogio;
    }

    @Override
    public void validarLote(LoteEnvioDTO lote) {
        if (lote.pacientesOuVazio().size() + lote.avaliacoesOuVazio().size() > LIMITE_DO_LOTE) {
            throw new FieldValidationException("lote", MENSAGEM_LOTE_GRANDE);
        }
    }

    @Override
    public Optional<String> motivoDaRecusa(String tipo, RegistroSyncDTO registro) {
        boolean avaliacao = ResultadoRegistroDTO.AVALIACAO.equals(tipo);
        if (!ehUuid(registro.id()) || (avaliacao && !ehUuid(registro.pacienteId()))) {
            return Optional.of(MENSAGEM_ID);
        }
        if (!dataValida(registro.atualizadoEm())) {
            return Optional.of(MENSAGEM_DATA);
        }
        JsonNode conteudo = registro.conteudo();
        if (conteudo == null || !conteudo.isObject() || !idDoConteudoConfere(avaliacao, registro)) {
            return Optional.of(MENSAGEM_CONTEUDO);
        }
        if (temChaveDeVideo(conteudo)) {
            return Optional.of(MENSAGEM_VIDEO);
        }
        return Optional.empty();
    }

    private boolean dataValida(String texto) {
        if (texto == null) return false;
        try {
            return !Instant.parse(texto).isAfter(Instant.now(relogio).plus(TOLERANCIA_FUTURO));
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    /** O id dentro do conteúdo tem de ser o mesmo do registro (na avaliação, o da linha de avaliação). */
    private static boolean idDoConteudoConfere(boolean avaliacao, RegistroSyncDTO registro) {
        JsonNode linha = avaliacao ? registro.conteudo().get("avaliacao") : registro.conteudo();
        if (linha == null || !linha.isObject()) return false;
        JsonNode id = linha.get("id");
        return id != null && id.isString() && registro.id().equalsIgnoreCase(id.asString());
    }

    /** D14: nenhum vídeo sobe; recusa chave com "video" ou "uri" no nome, em qualquer nível. */
    static boolean temChaveDeVideo(JsonNode no) {
        if (no == null) return false;
        if (no.isObject()) {
            for (Map.Entry<String, JsonNode> campo : no.properties()) {
                String nome = campo.getKey().toLowerCase(Locale.ROOT);
                if (nome.contains("video") || nome.contains("uri") || temChaveDeVideo(campo.getValue())) {
                    return true;
                }
            }
        } else if (no.isArray()) {
            for (JsonNode item : no) {
                if (temChaveDeVideo(item)) return true;
            }
        }
        return false;
    }

    private static boolean ehUuid(String texto) {
        if (texto == null) return false;
        try {
            UUID.fromString(texto);
            return texto.length() == 36;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
