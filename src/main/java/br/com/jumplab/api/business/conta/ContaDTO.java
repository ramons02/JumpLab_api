package br.com.jumplab.api.business.conta;

/** O que sai da conta para o app; sem id, sem hash. */
public record ContaDTO(String nome, String email, boolean admin) {}
