package br.com.jumplab.api.core.seguranca;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JwtServiceTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-bytes-ok";
    private static final String OUTRO_SEGREDO = "outro-segredo-de-teste-com-32-bytes-ou-mais";

    private final JwtService servico = new JwtService(new PropriedadesJwt(SEGREDO, 15, 30));

    @Test
    void tokenEmitidoELidoComOsDadosDaConta() {
        Jwt jwt = servico.ler(servico.emitir(7L, "ana@exemplo.com", true));

        assertEquals("7", jwt.getSubject());
        assertEquals("ana@exemplo.com", jwt.getClaimAsString(JwtService.CLAIM_EMAIL));
        assertTrue(jwt.getClaimAsBoolean(JwtService.CLAIM_ADMIN));
        assertEquals(Duration.ofMinutes(15), Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt()));
    }

    @Test
    void contaSemAdminLevaAdminFalso() {
        Jwt jwt = servico.ler(servico.emitir(7L, "ana@exemplo.com", false));

        assertFalse(jwt.getClaimAsBoolean(JwtService.CLAIM_ADMIN));
    }

    @Test
    void validadeEmSegundosVemDosMinutosConfigurados() {
        assertEquals(900, servico.validadeAcessoEmSegundos());
    }

    @Test
    void tokenVencidoERecusado() {
        String vencido = servico.emitir(7L, "ana@exemplo.com", false, Instant.now().minus(Duration.ofHours(1)));

        assertThrows(JwtException.class, () -> servico.ler(vencido));
    }

    @Test
    void tokenAssinadoComOutroSegredoERecusado() {
        JwtService outro = new JwtService(new PropriedadesJwt(OUTRO_SEGREDO, 15, 30));
        String token = outro.emitir(7L, "ana@exemplo.com", false);

        assertThrows(JwtException.class, () -> servico.ler(token));
    }

    @Test
    void tokenMalformadoERecusado() {
        assertThrows(JwtException.class, () -> servico.ler("isto-nao-e-um-jwt"));
    }

    @Test
    void segredoAusenteOuCurtoImpedeASubida() {
        assertThrows(IllegalStateException.class, () -> new PropriedadesJwt(null, 15, 30));
        assertThrows(IllegalStateException.class, () -> new PropriedadesJwt("curto-demais", 15, 30));
    }

    @Test
    void temposNaoPositivosImpedemASubida() {
        assertThrows(IllegalStateException.class, () -> new PropriedadesJwt(SEGREDO, 0, 30));
        assertThrows(IllegalStateException.class, () -> new PropriedadesJwt(SEGREDO, 15, 0));
    }
}
