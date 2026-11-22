package br.com.jumplab.api.business.conta;

import br.com.jumplab.api.core.controllers.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AutenticacaoController {

    static final String MENSAGEM_CONTA_CRIADA = "Conta criada.";

    private final IContaService contaService;

    public AutenticacaoController(IContaService contaService) {
        this.contaService = contaService;
    }

    @PostMapping("/cadastro")
    public ResponseEntity<ApiResponse<SessaoDTO>> cadastrar(@RequestBody CadastroRequest pedido) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.criado(MENSAGEM_CONTA_CRIADA, contaService.cadastrar(pedido)));
    }

    @PostMapping("/entrar")
    public ResponseEntity<ApiResponse<SessaoDTO>> entrar(@RequestBody EntrarRequest pedido) {
        return ResponseEntity.ok(ApiResponse.ok(contaService.entrar(pedido)));
    }

    @PostMapping("/renovar")
    public ResponseEntity<ApiResponse<SessaoDTO>> renovar(@RequestBody RenovarRequest pedido) {
        return ResponseEntity.ok(ApiResponse.ok(contaService.renovar(pedido)));
    }

    @PostMapping("/sair")
    public ResponseEntity<Void> sair(@RequestBody RenovarRequest pedido) {
        contaService.sair(pedido);
        return ResponseEntity.noContent().build();
    }
}
