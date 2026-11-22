package br.com.jumplab.api.business.conta;

/** trocaSenhaObrigatoria: sessão aberta pela senha temporária, que só pode criar a senha nova (spec 016). */
public record SessaoDTO(String tokenAcesso, String tokenRenovacao, long expiraEmSegundos,
                        boolean trocaSenhaObrigatoria, ContaDTO conta) {}
