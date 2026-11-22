package br.com.jumplab.api.business.sincronizacao;

import java.time.Instant;
import java.util.UUID;

/**
 * Linha de {@code paciente_sync} ou {@code avaliacao_sync} ({@code pacienteId} nulo no paciente).
 * Não estende o {@code BaseModel}: o id é o UUID do aparelho e não há soft delete (research R8).
 */
public record RegistroSyncModel(
    UUID id, UUID pacienteId, Instant atualizadoEm, String conteudo, long versao) {}
