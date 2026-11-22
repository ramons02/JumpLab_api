package br.com.jumplab.api.business.conta;

import br.com.jumplab.api.core.repositories.IGenericRepository;

import java.util.Optional;

public interface ITokenRenovacaoRepository extends IGenericRepository<TokenRenovacaoModel> {

    /** Só devolve token que não foi usado, não foi revogado e ainda não venceu. */
    Optional<TokenRenovacaoModel> buscarValidoPorHash(String tokenHash);

    /** Marca como usado se ainda não estava; falso quando outra renovação chegou antes. */
    boolean marcarUsado(Long id);

    void revogar(String tokenHash);

    /** Revoga as sessões de troca pendente do usuário, depois de ele criar a senha nova. */
    void revogarPendentesDoUsuario(Long usuarioId);
}
