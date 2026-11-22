package br.com.jumplab.api.business.conta;

final class UsuarioMapper {

    private UsuarioMapper() {}

    static ContaDTO paraConta(UsuarioModel usuario) {
        return new ContaDTO(usuario.getNome(), usuario.getEmail(), usuario.isAdmin());
    }
}
