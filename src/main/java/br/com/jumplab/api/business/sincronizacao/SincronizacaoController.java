package br.com.jumplab.api.business.sincronizacao;

import br.com.jumplab.api.core.controllers.response.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Rotas da sincronização. O usuário é sempre o do token ({@code sub}); nenhum parâmetro informa usuário. */
@RestController
@RequestMapping("/api/sync")
public class SincronizacaoController {

    private final ISincronizacaoService servico;

    public SincronizacaoController(ISincronizacaoService servico) {
        this.servico = servico;
    }

    @PostMapping("/enviar")
    public ApiResponse<ResultadoEnvioDTO> enviar(@AuthenticationPrincipal Jwt token, @RequestBody LoteEnvioDTO lote) {
        return ApiResponse.ok(servico.enviar(usuario(token), lote));
    }

    @GetMapping("/receber")
    public ApiResponse<PaginaRecebimentoDTO> receber(
        @AuthenticationPrincipal Jwt token,
        @RequestParam(defaultValue = "0") long desde,
        @RequestParam(defaultValue = "" + SincronizacaoService.LIMITE_PADRAO) int limite) {
        return ApiResponse.ok(servico.receber(usuario(token), desde, limite));
    }

    private static long usuario(Jwt token) {
        return Long.parseLong(token.getSubject());
    }
}
