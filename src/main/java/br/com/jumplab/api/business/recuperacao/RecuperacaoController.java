package br.com.jumplab.api.business.recuperacao;

import br.com.jumplab.api.core.controllers.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/recuperacao")
public class RecuperacaoController {

    static final String MENSAGEM_PEDIDO_ENVIADO =
        "Pedido enviado. O administrador vai gerar uma senha temporária para você.";

    private final IRecuperacaoService recuperacao;

    public RecuperacaoController(IRecuperacaoService recuperacao) {
        this.recuperacao = recuperacao;
    }

    /** Pública; 202 com o mesmo corpo, tenha o e-mail conta ou não (RN02). */
    @PostMapping("/pedidos")
    public ResponseEntity<ApiResponse<Void>> pedir(@RequestBody PedidoRequest pedido) {
        recuperacao.pedir(pedido);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(ApiResponse.sucesso(MENSAGEM_PEDIDO_ENVIADO, null));
    }
}
