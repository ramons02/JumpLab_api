package br.com.jumplab.api.business.recuperacao;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/** Senha temporária no formato XXXX-XXXX, sem caracteres ambíguos (0, O, 1, I, L) (research R4). */
@Component
public class GeradorSenhaTemporaria {

    static final String ALFABETO = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int TAMANHO_BLOCO = 4;

    private final SecureRandom aleatorio = new SecureRandom();

    public String gerar() {
        return bloco() + "-" + bloco();
    }

    private String bloco() {
        StringBuilder texto = new StringBuilder(TAMANHO_BLOCO);
        for (int i = 0; i < TAMANHO_BLOCO; i++) {
            texto.append(ALFABETO.charAt(aleatorio.nextInt(ALFABETO.length())));
        }
        return texto.toString();
    }
}
