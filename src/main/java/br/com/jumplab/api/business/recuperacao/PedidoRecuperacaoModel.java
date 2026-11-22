package br.com.jumplab.api.business.recuperacao;

import br.com.jumplab.api.core.domains.BaseModel;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class PedidoRecuperacaoModel extends BaseModel {

    public static final String ABERTO = "aberto";
    public static final String ATENDIDO = "atendido";

    private Long usuarioId;
    private Instant pedidoEm;
    private String status;
    private Instant atendidoEm;
}
