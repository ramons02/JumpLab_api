package br.com.jumplab.api.core.seguranca;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.charset.StandardCharsets;

/**
 * Configuração dos tokens, lida de {@code jumplab.jwt.*}. A API não sobe sem um segredo de pelo menos
 * 32 bytes, que é o tamanho da chave do HS256.
 */
@ConfigurationProperties(prefix = "jumplab.jwt")
public record PropriedadesJwt(String segredo, int acessoMinutos, int renovacaoDias) {

    public static final int TAMANHO_MINIMO_SEGREDO_BYTES = 32;

    public PropriedadesJwt {
        if (segredo == null || segredo.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_SEGREDO_BYTES) {
            throw new IllegalStateException(
                "JUMPLAB_JWT_SEGREDO ausente ou com menos de " + TAMANHO_MINIMO_SEGREDO_BYTES + " bytes.");
        }
        if (acessoMinutos <= 0 || renovacaoDias <= 0) {
            throw new IllegalStateException("Os tempos de expiração dos tokens precisam ser positivos.");
        }
    }
}
