package br.com.jumplab.api.business.recuperacao;

import java.time.Instant;

/** A senha em texto só existe nesta resposta; não é guardada nem registrada em log. */
public record SenhaTemporariaGeradaDTO(Long pedidoId, String email, String senhaTemporaria, Instant validaAte) {}
