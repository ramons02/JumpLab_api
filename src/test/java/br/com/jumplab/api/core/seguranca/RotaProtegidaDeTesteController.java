package br.com.jumplab.api.core.seguranca;

import br.com.jumplab.api.core.controllers.response.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Só existe nos testes: uma rota protegida para provar que o token de acesso vale (ou não). */
@RestController
public class RotaProtegidaDeTesteController {

    public static final String CAMINHO = "/api/teste/protegida";

    @GetMapping(CAMINHO)
    public ApiResponse<String> quemSouEu(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(jwt.getClaimAsString(JwtService.CLAIM_EMAIL));
    }
}
