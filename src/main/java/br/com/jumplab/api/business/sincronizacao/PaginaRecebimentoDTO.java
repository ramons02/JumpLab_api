package br.com.jumplab.api.business.sincronizacao;

import java.util.List;

/** Resposta de {@code GET /api/sync/receber}: registros com versão maior que o cursor pedido. */
public record PaginaRecebimentoDTO(
    List<RegistroSyncDTO> pacientes, List<RegistroSyncDTO> avaliacoes, long cursor, boolean temMais) {}
