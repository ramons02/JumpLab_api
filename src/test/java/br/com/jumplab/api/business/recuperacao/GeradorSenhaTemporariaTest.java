package br.com.jumplab.api.business.recuperacao;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeradorSenhaTemporariaTest {

    private final GeradorSenhaTemporaria gerador = new GeradorSenhaTemporaria();

    @Test
    void formatoXXXXmenosXXXXSemCaracteresAmbiguos() {
        for (int i = 0; i < 1000; i++) {
            String senha = gerador.gerar();
            assertTrue(senha.matches("^[A-Z2-9]{4}-[A-Z2-9]{4}$"), senha);
            assertFalse(senha.matches(".*[01OIL].*"), senha);
        }
    }

    @Test
    void dezMilGeracoesSemRepeticao() {
        Set<String> geradas = new HashSet<>();
        for (int i = 0; i < 10_000; i++) {
            geradas.add(gerador.gerar());
        }
        assertEquals(10_000, geradas.size());
    }
}
