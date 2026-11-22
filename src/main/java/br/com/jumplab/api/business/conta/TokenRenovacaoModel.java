package br.com.jumplab.api.business.conta;

import br.com.jumplab.api.core.domains.BaseModel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Token de renovação guardado só como hash SHA-256; o valor em si fica apenas com o app. */
@Getter
@Setter
public class TokenRenovacaoModel extends BaseModel {

    private Long usuarioId;
    private String tokenHash;
    private Instant emitidoEm;
    private Instant expiraEm;
    private Instant usadoEm;
    private Instant revogadoEm;
}
