package br.com.jumplab.api.business.sincronizacao;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Gravação "vence a alteração mais recente" (D15) e leitura incremental por versão. Não estende o
 * {@code AbstractRepository}: o CRUD genérico (id Long, soft delete) não serve à troca (research R8).
 */
public interface ISyncRepository {

    /** Insere ou atualiza se for mais recente e da mesma conta; devolve true se gravou. */
    boolean gravarSeMaisRecente(long usuarioId, UUID id, UUID pacienteId, Instant atualizadoEm, String conteudo);

    /** O id já existe com outro dono. */
    boolean pertenceAOutroUsuario(UUID id, long usuarioId);

    /** Registros da conta com versão maior que {@code desde}, em ordem de versão. */
    List<RegistroSyncModel> listarDesde(long usuarioId, long desde, int limite);
}
