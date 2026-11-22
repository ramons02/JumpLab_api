package br.com.jumplab.api.core.seguranca;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

/** Emite e lê o token de acesso (JWT HS256). O token de renovação é opaco e fica na conta. */
@Service
public class JwtService {

    public static final String CLAIM_EMAIL = "email";
    public static final String CLAIM_ADMIN = "admin";
    /** Só presente (e verdadeiro) na sessão aberta pela senha temporária (spec 016). */
    public static final String CLAIM_TROCA_SENHA = "troca_senha";

    private static final String ALGORITMO_CHAVE = "HmacSHA256";

    private final JwtEncoder codificador;
    private final JwtDecoder decodificador;
    private final Duration validadeAcesso;

    public JwtService(PropriedadesJwt propriedades) {
        SecretKey chave = new SecretKeySpec(
            propriedades.segredo().getBytes(StandardCharsets.UTF_8), ALGORITMO_CHAVE);
        this.codificador = new NimbusJwtEncoder(new ImmutableSecret<>(chave));
        this.decodificador = NimbusJwtDecoder.withSecretKey(chave).macAlgorithm(MacAlgorithm.HS256).build();
        this.validadeAcesso = Duration.ofMinutes(propriedades.acessoMinutos());
    }

    public String emitir(Long usuarioId, String email, boolean admin) {
        return emitir(usuarioId, email, admin, false, Instant.now());
    }

    public String emitir(Long usuarioId, String email, boolean admin, boolean trocaSenhaPendente) {
        return emitir(usuarioId, email, admin, trocaSenhaPendente, Instant.now());
    }

    /** Emite como se fosse no instante informado; a expiração conta a partir dele. */
    public String emitir(Long usuarioId, String email, boolean admin, Instant emitidoEm) {
        return emitir(usuarioId, email, admin, false, emitidoEm);
    }

    private String emitir(Long usuarioId, String email, boolean admin, boolean trocaSenhaPendente,
                          Instant emitidoEm) {
        JwtClaimsSet.Builder construtor = JwtClaimsSet.builder()
            .subject(String.valueOf(usuarioId))
            .claim(CLAIM_EMAIL, email)
            .claim(CLAIM_ADMIN, admin)
            .issuedAt(emitidoEm)
            .expiresAt(emitidoEm.plus(validadeAcesso));
        if (trocaSenhaPendente) {
            construtor.claim(CLAIM_TROCA_SENHA, true);
        }
        JwtClaimsSet claims = construtor.build();
        JwsHeader cabecalho = JwsHeader.with(MacAlgorithm.HS256).build();
        return codificador.encode(JwtEncoderParameters.from(cabecalho, claims)).getTokenValue();
    }

    /** Lança {@link org.springframework.security.oauth2.jwt.JwtException} se o token não valer. */
    public Jwt ler(String token) {
        return decodificador.decode(token);
    }

    public long validadeAcessoEmSegundos() {
        return validadeAcesso.toSeconds();
    }

    JwtDecoder getDecodificador() {
        return decodificador;
    }
}
