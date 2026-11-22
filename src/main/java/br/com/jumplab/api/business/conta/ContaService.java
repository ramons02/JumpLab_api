package br.com.jumplab.api.business.conta;

import br.com.jumplab.api.core.exceptions.BusinessException;
import br.com.jumplab.api.core.exceptions.CredenciaisInvalidasException;
import br.com.jumplab.api.core.exceptions.SessaoInvalidaException;
import br.com.jumplab.api.core.seguranca.JwtService;
import br.com.jumplab.api.core.seguranca.PropriedadesJwt;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Optional;

@Service
public class ContaService implements IContaService {

    static final String MENSAGEM_EMAIL_REPETIDO = "Já existe conta com este e-mail.";

    private static final int TAMANHO_TOKEN_RENOVACAO_BYTES = 32;
    /** Senha usada só para gerar o hash comparado quando o e-mail não existe (tempo igual). */
    private static final String SENHA_DE_REFERENCIA = "senha-de-referencia-para-email-inexistente";

    private final IUsuarioRepository usuarios;
    private final ITokenRenovacaoRepository tokens;
    private final IContaValidation validacao;
    private final PasswordEncoder codificadorDeSenha;
    private final JwtService jwtService;
    private final Duration validadeRenovacao;
    private final SecureRandom aleatorio = new SecureRandom();
    private final String hashDeReferencia;

    public ContaService(IUsuarioRepository usuarios, ITokenRenovacaoRepository tokens,
                        IContaValidation validacao, PasswordEncoder codificadorDeSenha,
                        JwtService jwtService, PropriedadesJwt propriedadesJwt) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.validacao = validacao;
        this.codificadorDeSenha = codificadorDeSenha;
        this.jwtService = jwtService;
        this.validadeRenovacao = Duration.ofDays(propriedadesJwt.renovacaoDias());
        this.hashDeReferencia = codificadorDeSenha.encode(SENHA_DE_REFERENCIA);
    }

    @Override
    @Transactional
    public SessaoDTO cadastrar(CadastroRequest pedido) {
        CadastroRequest normalizado = new CadastroRequest(
            pedido.nome() == null ? null : pedido.nome().strip(),
            normalizarEmail(pedido.email()),
            pedido.senha());
        validacao.validar(normalizado);

        UsuarioModel novo = new UsuarioModel();
        novo.setNome(normalizado.nome());
        novo.setEmail(normalizado.email());
        novo.setSenhaHash(codificadorDeSenha.encode(normalizado.senha()));
        return emitirSessao(salvarSemRepetirEmail(novo));
    }

    @Override
    @Transactional
    public SessaoDTO entrar(EntrarRequest pedido) {
        if (pedido == null || pedido.email() == null || !senhaAceitaPeloBcrypt(pedido.senha())) {
            throw new CredenciaisInvalidasException();
        }
        Optional<UsuarioModel> usuario = usuarios.buscarPorEmail(normalizarEmail(pedido.email()));
        String hash = usuario.map(UsuarioModel::getSenhaHash).orElse(hashDeReferencia);
        boolean senhaConfere = codificadorDeSenha.matches(pedido.senha(), hash);
        if (usuario.isEmpty() || !senhaConfere || !usuario.get().isAtivo()) {
            throw new CredenciaisInvalidasException();
        }
        return emitirSessao(usuario.get());
    }

    @Override
    @Transactional
    public SessaoDTO renovar(RenovarRequest pedido) {
        String valor = pedido == null ? null : pedido.tokenRenovacao();
        if (valor == null || valor.isBlank()) {
            throw new SessaoInvalidaException();
        }
        TokenRenovacaoModel token = tokens.buscarValidoPorHash(sha256(valor))
            .orElseThrow(SessaoInvalidaException::new);
        if (!tokens.marcarUsado(token.getId())) {
            throw new SessaoInvalidaException();
        }
        UsuarioModel usuario = usuarios.buscarPorId(token.getUsuarioId())
            .filter(UsuarioModel::isAtivo)
            .orElseThrow(SessaoInvalidaException::new);
        return emitirSessao(usuario);
    }

    @Override
    @Transactional
    public void sair(RenovarRequest pedido) {
        if (pedido != null && pedido.tokenRenovacao() != null && !pedido.tokenRenovacao().isBlank()) {
            tokens.revogar(sha256(pedido.tokenRenovacao()));
        }
    }

    private UsuarioModel salvarSemRepetirEmail(UsuarioModel novo) {
        try {
            return usuarios.salvar(novo);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(MENSAGEM_EMAIL_REPETIDO);
        }
    }

    private SessaoDTO emitirSessao(UsuarioModel usuario) {
        String tokenAcesso = jwtService.emitir(usuario.getId(), usuario.getEmail(), usuario.isAdmin());
        String tokenRenovacao = gerarTokenOpaco();
        Instant agora = Instant.now();

        TokenRenovacaoModel registro = new TokenRenovacaoModel();
        registro.setUsuarioId(usuario.getId());
        registro.setTokenHash(sha256(tokenRenovacao));
        registro.setEmitidoEm(agora);
        registro.setExpiraEm(agora.plus(validadeRenovacao));
        tokens.salvar(registro);

        return new SessaoDTO(tokenAcesso, tokenRenovacao, jwtService.validadeAcessoEmSegundos(),
            UsuarioMapper.paraConta(usuario));
    }

    private String gerarTokenOpaco() {
        byte[] bytes = new byte[TAMANHO_TOKEN_RENOVACAO_BYTES];
        aleatorio.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean senhaAceitaPeloBcrypt(String senha) {
        return senha != null
            && senha.getBytes(StandardCharsets.UTF_8).length <= ContaValidation.SENHA_MAXIMA_BYTES;
    }

    static String normalizarEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
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
