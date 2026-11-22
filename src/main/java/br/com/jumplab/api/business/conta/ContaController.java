package br.com.jumplab.api.business.conta;

import br.com.jumplab.api.core.controllers.response.ApiResponse;
import br.com.jumplab.api.core.seguranca.FiltroTrocaSenhaPendente;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/conta")
public class ContaController {

    static final String MENSAGEM_SENHA_NOVA = "Senha nova salva. A senha temporária deixou de valer.";

    private final IContaService contaService;

    public ContaController(IContaService contaService) {
        this.contaService = contaService;
    }

    /** Só para a sessão aberta pela senha temporária: troca voluntária está fora do escopo (R3). */
    @PostMapping("/senha")
    public ResponseEntity<ApiResponse<SessaoDTO>> criarSenhaNova(@AuthenticationPrincipal Jwt jwt,
                                                                 @RequestBody SenhaNovaRequest pedido) {
        if (!FiltroTrocaSenhaPendente.trocaPendente()) {
            throw new AccessDeniedException("Sessão sem troca de senha pendente.");
        }
        SessaoDTO sessao = contaService.criarSenhaNova(Long.valueOf(jwt.getSubject()), pedido);
        return ResponseEntity.ok(ApiResponse.sucesso(MENSAGEM_SENHA_NOVA, sessao));
    }
}
