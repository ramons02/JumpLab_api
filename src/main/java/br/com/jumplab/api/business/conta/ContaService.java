package br.com.jumplab.api.business.conta;

import br.com.jumplab.api.business.recuperacao.ISenhaTemporariaRepository;
import br.com.jumplab.api.business.recuperacao.SenhaTemporariaModel;
import br.com.jumplab.api.core.exceptions.BusinessException;
import br.com.jumplab.api.core.exceptions.CredenciaisInvalidasException;
import br.com.jumplab.api.core.exceptions.SessaoInvalidaException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

@Service
public class ContaService implements IContaService {

    static final String MENSAGEM_EMAIL_REPETIDO = "Já existe conta com este e-mail.";

    /** Senha usada só para gerar o hash comparado quando o e-mail não existe (tempo igual). */
    private static final String SENHA_DE_REFERENCIA = "senha-de-referencia-para-email-inexistente";

    private final IUsuarioRepository usuarios;
    private final ITokenRenovacaoRepository tokens;
    private final ISenhaTemporariaRepository senhasTemporarias;
    private final IContaValidation validacao;
    private final PasswordEncoder codificadorDeSenha;
    private final EmissorSessao emissor;
    private final String hashDeReferencia;

    public ContaService(IUsuarioRepository usuarios, ITokenRenovacaoRepository tokens,
                        ISenhaTemporariaRepository senhasTemporarias, IContaValidation validacao,
                        PasswordEncoder codificadorDeSenha, EmissorSessao emissor) {
        this.usuarios = usuarios;
        this.tokens = tokens;
        this.senhasTemporarias = senhasTemporarias;
        this.validacao = validacao;
        this.codificadorDeSenha = codificadorDeSenha;
        this.emissor = emissor;
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
        return emissor.emitir(salvarSemRepetirEmail(novo), false);
    }

    /**
     * A senha normal vale sempre (HU016 RN10). Se ela não bater, uma senha temporária válida abre a
     * sessão com troca pendente e deixa de valer (uma entrada, research R1).
     */
    @Override
    @Transactional
    public SessaoDTO entrar(EntrarRequest pedido) {
        if (pedido == null || pedido.email() == null || !senhaAceitaPeloBcrypt(pedido.senha())) {
            throw new CredenciaisInvalidasException();
        }
        Optional<UsuarioModel> usuario = usuarios.buscarPorEmail(normalizarEmail(pedido.email()));
        String hash = usuario.map(UsuarioModel::getSenhaHash).orElse(hashDeReferencia);
        boolean senhaConfere = codificadorDeSenha.matches(pedido.senha(), hash);
        if (usuario.isEmpty() || !usuario.get().isAtivo()) {
            throw new CredenciaisInvalidasException();
        }
        if (senhaConfere) {
            return emissor.emitir(usuario.get(), false);
        }
        if (entrouPelaSenhaTemporaria(usuario.get().getId(), pedido.senha())) {
            return emissor.emitir(usuario.get(), true);
        }
        throw new CredenciaisInvalidasException();
    }

    @Override
    @Transactional
    public SessaoDTO renovar(RenovarRequest pedido) {
        String valor = pedido == null ? null : pedido.tokenRenovacao();
        if (valor == null || valor.isBlank()) {
            throw new SessaoInvalidaException();
        }
        TokenRenovacaoModel token = tokens.buscarValidoPorHash(EmissorSessao.sha256(valor))
            .orElseThrow(SessaoInvalidaException::new);
        if (!tokens.marcarUsado(token.getId())) {
            throw new SessaoInvalidaException();
        }
        UsuarioModel usuario = usuarios.buscarPorId(token.getUsuarioId())
            .filter(UsuarioModel::isAtivo)
            .orElseThrow(SessaoInvalidaException::new);
        return emissor.emitir(usuario, token.isTrocaSenhaPendente());
    }

    @Override
    @Transactional
    public void sair(RenovarRequest pedido) {
        if (pedido != null && pedido.tokenRenovacao() != null && !pedido.tokenRenovacao().isBlank()) {
            tokens.revogar(EmissorSessao.sha256(pedido.tokenRenovacao()));
        }
    }

    /**
     * Grava a senha nova de quem entrou pela temporária: as temporárias abertas e a sessão pendente deixam
     * de valer, e volta uma sessão normal (research R3).
     */
    @Override
    @Transactional
    public SessaoDTO criarSenhaNova(Long usuarioId, SenhaNovaRequest pedido) {
        ContaValidation.validarSenha(pedido == null ? null : pedido.senhaNova());
        UsuarioModel usuario = usuarios.buscarPorId(usuarioId)
            .filter(UsuarioModel::isAtivo)
            .orElseThrow(SessaoInvalidaException::new);
        usuario.setSenhaHash(codificadorDeSenha.encode(pedido.senhaNova()));
        usuarios.atualizar(usuario);
        senhasTemporarias.invalidarAbertasDoUsuario(usuarioId);
        tokens.revogarPendentesDoUsuario(usuarioId);
        return emissor.emitir(usuario, false);
    }

    private boolean entrouPelaSenhaTemporaria(Long usuarioId, String senhaDigitada) {
        String normalizada = senhaDigitada.strip().toUpperCase(Locale.ROOT);
        for (SenhaTemporariaModel temporaria : senhasTemporarias.listarValidasDoUsuario(usuarioId)) {
            if (codificadorDeSenha.matches(normalizada, temporaria.getSenhaHash())) {
                return senhasTemporarias.marcarUsada(temporaria.getId());
            }
        }
        return false;
    }

    private UsuarioModel salvarSemRepetirEmail(UsuarioModel novo) {
        try {
            return usuarios.salvar(novo);
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(MENSAGEM_EMAIL_REPETIDO);
        }
    }

    private static boolean senhaAceitaPeloBcrypt(String senha) {
        return senha != null
            && senha.getBytes(StandardCharsets.UTF_8).length <= ContaValidation.SENHA_MAXIMA_BYTES;
    }

    public static String normalizarEmail(String email) {
        return email == null ? null : email.strip().toLowerCase(Locale.ROOT);
    }
}
