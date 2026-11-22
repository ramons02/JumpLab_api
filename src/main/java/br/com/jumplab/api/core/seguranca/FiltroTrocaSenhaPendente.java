package br.com.jumplab.api.core.seguranca;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Sessão aberta pela senha temporária só pode criar a senha nova (spec 016, R2): qualquer outra rota
 * autenticada recebe 403. As rotas de /api/auth seguem livres para renovar e sair.
 */
public class FiltroTrocaSenhaPendente extends OncePerRequestFilter {

    public static final String MENSAGEM = "Crie sua senha nova para continuar.";
    public static final String ROTA_SENHA_NOVA = "/api/conta/senha";
    private static final String PREFIXO_AUTENTICACAO = "/api/auth/";

    private final TratadorAcessoNegado tratador;

    public FiltroTrocaSenhaPendente(TratadorAcessoNegado tratador) {
        this.tratador = tratador;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain cadeia) throws ServletException, IOException {
        if (trocaPendente() && !rotaLiberada(request)) {
            tratador.escrever(response, HttpStatus.FORBIDDEN, MENSAGEM);
            return;
        }
        cadeia.doFilter(request, response);
    }

    /** O token de acesso desta requisição tem o claim troca_senha. */
    public static boolean trocaPendente() {
        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        return autenticacao instanceof JwtAuthenticationToken jwt
            && Boolean.TRUE.equals(jwt.getToken().getClaimAsBoolean(JwtService.CLAIM_TROCA_SENHA));
    }

    private static boolean rotaLiberada(HttpServletRequest request) {
        String caminho = request.getRequestURI().substring(request.getContextPath().length());
        boolean senhaNova = HttpMethod.POST.matches(request.getMethod()) && caminho.equals(ROTA_SENHA_NOVA);
        return senhaNova || caminho.startsWith(PREFIXO_AUTENTICACAO);
    }
}
