package br.com.jumplab.api;

import br.com.jumplab.api.core.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JumpLabApiApplicationIT extends AbstractIntegrationTest {

    @Test
    void sobeAAplicacaoConectadaAoPostgres() {
        assertEquals(1, jdbc.queryForObject("SELECT 1", Integer.class));
    }
}
