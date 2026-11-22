package br.com.jumplab.api.business.conta;

import br.com.jumplab.api.core.seguranca.JwtService;
import br.com.jumplab.api.core.seguranca.PropriedadesJwt;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Emite o par de tokens de uma sessão: o de acesso (JWT) e o de renovação, opaco, guardado só como
 * SHA-256. A sessão aberta pela senha temporária leva a marca de troca pendente nos dois (spec 016).
 */
@Component
public class EmissorSessao {

    private static final int TAMANHO_TOKEN_RENOVACAO_BYTES = 32;

    private final ITokenRenovacaoRepository tokens;
    private final JwtService jwtService;
    private final Duration validadeRenovacao;
    private final SecureRandom aleatorio = new SecureRandom();

    public EmissorSessao(ITokenRenovacaoRepository tokens, JwtService jwtService, PropriedadesJwt propriedadesJwt) {
        this.tokens = tokens;
        this.jwtService = jwtService;
        this.validadeRenovacao = Duration.ofDays(propriedadesJwt.renovacaoDias());
    }

    public SessaoDTO emitir(UsuarioModel usuario, boolean trocaSenhaPendente) {
        String tokenAcesso = jwtService.emitir(
            usuario.getId(), usuario.getEmail(), usuario.isAdmin(), trocaSenhaPendente);
        String tokenRenovacao = gerarTokenOpaco();
        Instant agora = Instant.now();

        TokenRenovacaoModel registro = new TokenRenovacaoModel();
        registro.setUsuarioId(usuario.getId());
        registro.setTokenHash(sha256(tokenRenovacao));
        registro.setEmitidoEm(agora);
        registro.setExpiraEm(agora.plus(validadeRenovacao));
        registro.setTrocaSenhaPendente(trocaSenhaPendente);
        tokens.salvar(registro);

        return new SessaoDTO(tokenAcesso, tokenRenovacao, jwtService.validadeAcessoEmSegundos(),
            trocaSenhaPendente, UsuarioMapper.paraConta(usuario));
    }

    private String gerarTokenOpaco() {
        byte[] bytes = new byte[TAMANHO_TOKEN_RENOVACAO_BYTES];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String sha256(String valor) {
        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(resumo);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível na JVM", e);
        }
    }
}
