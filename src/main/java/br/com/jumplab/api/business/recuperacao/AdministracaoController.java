package br.com.jumplab.api.business.recuperacao;

import br.com.jumplab.api.core.controllers.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Caixa de pedidos do administrador; /api/admin/** exige ROLE_ADMIN (ConfiguracaoSeguranca). */
@RestController
@RequestMapping("/api/admin/pedidos")
public class AdministracaoController {

    private final IRecuperacaoService recuperacao;

    public AdministracaoController(IRecuperacaoService recuperacao) {
        this.recuperacao = recuperacao;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PedidoRecuperacaoDTO>>> listar() {
        return ResponseEntity.ok(ApiResponse.ok(recuperacao.listarPedidos()));
    }

    @PostMapping("/{id}/senha-temporaria")
    public ResponseEntity<ApiResponse<SenhaTemporariaGeradaDTO>> gerarSenha(
        @PathVariable Long id, @AuthenticationPrincipal Jwt administrador) {
        SenhaTemporariaGeradaDTO gerada = recuperacao.gerarSenha(id, Long.valueOf(administrador.getSubject()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.sucesso(null, gerada));
    }
}
