package br.com.jumplab.api.business.conta;

import br.com.jumplab.api.core.domains.BaseModel;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UsuarioModel extends BaseModel {

    private String nome;
    private String email;
    private String senhaHash;
    private boolean admin;
}
