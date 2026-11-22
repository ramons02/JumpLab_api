package br.com.jumplab.api.business.recuperacao;

import java.time.Instant;

public record PedidoRecuperacaoDTO(Long id, String email, Instant pedidoEm, String status, Instant atendidoEm) {}
