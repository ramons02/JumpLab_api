package br.com.jumplab.api.business.conta;

public record SessaoDTO(String tokenAcesso, String tokenRenovacao, long expiraEmSegundos, ContaDTO conta) {}
