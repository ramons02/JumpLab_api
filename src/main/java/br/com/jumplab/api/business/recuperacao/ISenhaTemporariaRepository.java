package br.com.jumplab.api.business.recuperacao;

import br.com.jumplab.api.core.repositories.IGenericRepository;

import java.util.List;

public interface ISenhaTemporariaRepository extends IGenericRepository<SenhaTemporariaModel> {

    /** Não usadas, não invalidadas e dentro das 24 horas. */
    List<SenhaTemporariaModel> listarValidasDoUsuario(Long usuarioId);

    /** Marca a primeira e única entrada; falso se outra entrada chegou antes. */
    boolean marcarUsada(Long id);

    void invalidarAbertasDoUsuario(Long usuarioId);
}
