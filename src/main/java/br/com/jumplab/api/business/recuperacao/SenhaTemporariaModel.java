package br.com.jumplab.api.business.recuperacao;

import br.com.jumplab.api.core.domains.BaseModel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

/** Guardada só como hash BCrypt; o texto existe apenas na resposta ao administrador. */
@Getter
@Setter
public class SenhaTemporariaModel extends BaseModel {

    private Long usuarioId;
    private Long pedidoId;
    private String senhaHash;
    private Long geradaPor;
    private Instant geradaEm;
    private Instant expiraEm;
    private Instant usadaEm;
    private Instant invalidadaEm;
}
