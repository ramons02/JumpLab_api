package br.com.jumplab.api.business.conta;

import br.com.jumplab.api.core.exceptions.BusinessException;
import br.com.jumplab.api.core.exceptions.CredenciaisInvalidasException;
import br.com.jumplab.api.core.exceptions.FieldValidationException;
import br.com.jumplab.api.core.exceptions.SessaoInvalidaException;
import br.com.jumplab.api.core.seguranca.JwtService;
import br.com.jumplab.api.core.seguranca.PropriedadesJwt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ContaServiceTest {

    private static final String HASH_REFERENCIA = "hash-de-referencia";
    private static final String HASH_DA_ANA = "hash-da-ana";

    private final IUsuarioRepository usuarios = mock(IUsuarioRepository.class);
    private final ITokenRenovacaoRepository tokens = mock(ITokenRenovacaoRepository.class);
    private final IContaValidation validacao = mock(IContaValidation.class);
    private final PasswordEncoder codificador = mock(PasswordEncoder.class);
    private final JwtService jwt = new JwtService(new PropriedadesJwt("segredo-de-teste-com-mais-de-32-bytes-ok", 15, 30));
    private ContaService servico;

    @BeforeEach
    void preparar() {
        when(codificador.encode(anyString())).thenAnswer(inv -> "bcrypt(" + inv.getArgument(0) + ")");
        when(codificador.encode("senha-de-referencia-para-email-inexistente")).thenReturn(HASH_REFERENCIA);
        when(usuarios.salvar(any())).thenAnswer(inv -> {
            UsuarioModel u = inv.getArgument(0);
            u.setId(1L);
            u.setAtivo(true);
            return u;
        });
        servico = new ContaService(usuarios, tokens, validacao, codificador, jwt,
            new PropriedadesJwt("segredo-de-teste-com-mais-de-32-bytes-ok", 15, 30));
    }

    // cadastro

    @Test
    void cadastroValidaAntesDeSalvar() {
        doThrow(new FieldValidationException("nome", "Informe seu nome.")).when(validacao).validar(any());

        assertThrows(FieldValidationException.class,
            () -> servico.cadastrar(new CadastroRequest("A", "ana@exemplo.com", "segredo123")));
        verify(usuarios, never()).salvar(any());
    }

    @Test
    void cadastroNormalizaEGravaSoOHashDaSenha() {
        servico.cadastrar(new CadastroRequest("  Ana Souza ", "  Ana@Exemplo.COM ", "segredo123"));

        ArgumentCaptor<CadastroRequest> validado = ArgumentCaptor.forClass(CadastroRequest.class);
        ArgumentCaptor<UsuarioModel> salvo = ArgumentCaptor.forClass(UsuarioModel.class);
        var ordem = inOrder(validacao, usuarios);
        ordem.verify(validacao).validar(validado.capture());
        ordem.verify(usuarios).salvar(salvo.capture());

        assertEquals("ana@exemplo.com", validado.getValue().email());
        assertEquals("Ana Souza", salvo.getValue().getNome());
        assertEquals("ana@exemplo.com", salvo.getValue().getEmail());
        assertEquals("bcrypt(segredo123)", salvo.getValue().getSenhaHash());
    }

    @Test
    void cadastroComEmailRepetidoViraRegraDeNegocio() {
        doThrow(new DataIntegrityViolationException("ux_usuario_email")).when(usuarios).salvar(any());

        BusinessException erro = assertThrows(BusinessException.class,
            () -> servico.cadastrar(new CadastroRequest("Ana", "ana@exemplo.com", "segredo123")));
        assertEquals("Já existe conta com este e-mail.", erro.getMessage());
    }

    @Test
    void cadastroDevolveSessaoComOsDoisTokens() {
        SessaoDTO sessao = servico.cadastrar(new CadastroRequest("Ana Souza", "ana@exemplo.com", "segredo123"));

        assertEquals("1", jwt.ler(sessao.tokenAcesso()).getSubject());
        assertEquals(900, sessao.expiraEmSegundos());
        assertEquals(new ContaDTO("Ana Souza", "ana@exemplo.com", false), sessao.conta());

        ArgumentCaptor<TokenRenovacaoModel> registro = ArgumentCaptor.forClass(TokenRenovacaoModel.class);
        verify(tokens).salvar(registro.capture());
        assertEquals(ContaService.sha256(sessao.tokenRenovacao()), registro.getValue().getTokenHash());
        assertNotEquals(sessao.tokenRenovacao(), registro.getValue().getTokenHash());
        assertEquals(Duration.ofDays(30),
            Duration.between(registro.getValue().getEmitidoEm(), registro.getValue().getExpiraEm()));
    }

    // entrar

    @Test
    void entrarComSenhaCerta() {
        when(usuarios.buscarPorEmail("ana@exemplo.com")).thenReturn(Optional.of(ana()));
        when(codificador.matches("segredo123", HASH_DA_ANA)).thenReturn(true);

        SessaoDTO sessao = servico.entrar(new EntrarRequest(" ANA@exemplo.com ", "segredo123"));

        assertEquals("ana@exemplo.com", sessao.conta().email());
    }

    @Test
    void senhaErradaEEmailInexistenteDaoAMesmaExcecao() {
        when(usuarios.buscarPorEmail("ana@exemplo.com")).thenReturn(Optional.of(ana()));
        when(usuarios.buscarPorEmail("ninguem@exemplo.com")).thenReturn(Optional.empty());

        var senhaErrada = assertThrows(CredenciaisInvalidasException.class,
            () -> servico.entrar(new EntrarRequest("ana@exemplo.com", "errada123")));
        var semConta = assertThrows(CredenciaisInvalidasException.class,
            () -> servico.entrar(new EntrarRequest("ninguem@exemplo.com", "segredo123")));

        assertEquals(senhaErrada.getMessage(), semConta.getMessage());
    }

    @Test
    void emailInexistenteTambemPassaPeloBcrypt() {
        when(usuarios.buscarPorEmail(anyString())).thenReturn(Optional.empty());

        assertThrows(CredenciaisInvalidasException.class,
            () -> servico.entrar(new EntrarRequest("ninguem@exemplo.com", "segredo123")));
        verify(codificador).matches("segredo123", HASH_REFERENCIA);
    }

    @Test
    void contaInativaNaoEntra() {
        UsuarioModel inativa = ana();
        inativa.setAtivo(false);
        when(usuarios.buscarPorEmail("ana@exemplo.com")).thenReturn(Optional.of(inativa));
        when(codificador.matches("segredo123", HASH_DA_ANA)).thenReturn(true);

        assertThrows(CredenciaisInvalidasException.class,
            () -> servico.entrar(new EntrarRequest("ana@exemplo.com", "segredo123")));
    }

    @Test
    void corpoIncompletoNaoEntra() {
        assertThrows(CredenciaisInvalidasException.class, () -> servico.entrar(new EntrarRequest(null, "x")));
        assertThrows(CredenciaisInvalidasException.class, () -> servico.entrar(new EntrarRequest("a@b.com", null)));
        assertThrows(CredenciaisInvalidasException.class,
            () -> servico.entrar(new EntrarRequest("a@b.com", "a".repeat(73))));
    }

    // renovar e sair

    @Test
    void renovarMarcaOTokenUsadoEEmiteOutro() {
        TokenRenovacaoModel atual = tokenDaAna();
        when(tokens.buscarValidoPorHash(ContaService.sha256("valor-atual"))).thenReturn(Optional.of(atual));
        when(tokens.marcarUsado(atual.getId())).thenReturn(true);
        when(usuarios.buscarPorId(1L)).thenReturn(Optional.of(ana()));

        SessaoDTO nova = servico.renovar(new RenovarRequest("valor-atual"));

        verify(tokens).marcarUsado(atual.getId());
        assertNotEquals("valor-atual", nova.tokenRenovacao());
        verify(tokens).salvar(any());
    }

    @Test
    void tokenUsadoRevogadoOuVencidoERecusado() {
        when(tokens.buscarValidoPorHash(anyString())).thenReturn(Optional.empty());

        assertThrows(SessaoInvalidaException.class, () -> servico.renovar(new RenovarRequest("qualquer")));
        verify(tokens, never()).salvar(any());
    }

    @Test
    void renovacaoQuePerdeACorridaERecusada() {
        TokenRenovacaoModel atual = tokenDaAna();
        when(tokens.buscarValidoPorHash(anyString())).thenReturn(Optional.of(atual));
        when(tokens.marcarUsado(atual.getId())).thenReturn(false);

        assertThrows(SessaoInvalidaException.class, () -> servico.renovar(new RenovarRequest("valor-atual")));
        verify(tokens, never()).salvar(any());
    }

    @Test
    void renovarSemTokenERecusado() {
        assertThrows(SessaoInvalidaException.class, () -> servico.renovar(new RenovarRequest(" ")));
        assertThrows(SessaoInvalidaException.class, () -> servico.renovar(null));
    }

    @Test
    void sairRevogaPeloHash() {
        servico.sair(new RenovarRequest("valor-atual"));

        verify(tokens).revogar(eq(ContaService.sha256("valor-atual")));
    }

    @Test
    void sairSemTokenNaoFazNada() {
        servico.sair(new RenovarRequest(null));

        verify(tokens, never()).revogar(anyString());
    }

    private static UsuarioModel ana() {
        UsuarioModel u = new UsuarioModel();
        u.setId(1L);
        u.setNome("Ana Souza");
        u.setEmail("ana@exemplo.com");
        u.setSenhaHash(HASH_DA_ANA);
        u.setAtivo(true);
        return u;
    }

    private static TokenRenovacaoModel tokenDaAna() {
        TokenRenovacaoModel t = new TokenRenovacaoModel();
        t.setId(10L);
        t.setUsuarioId(1L);
        return t;
    }
}
