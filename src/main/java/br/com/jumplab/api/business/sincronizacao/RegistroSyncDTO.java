package br.com.jumplab.api.business.sincronizacao;

import tools.jackson.databind.JsonNode;

/**
 * Um paciente ou uma avaliação na troca com o app. {@code pacienteId} só existe na avaliação;
 * {@code conteudo} vai e volta como o app enviou, sem a API interpretar os campos.
 */
public record RegistroSyncDTO(String id, String pacienteId, String atualizadoEm, JsonNode conteudo) {}
