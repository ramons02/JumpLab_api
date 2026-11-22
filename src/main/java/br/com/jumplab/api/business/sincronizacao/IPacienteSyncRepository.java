package br.com.jumplab.api.business.sincronizacao;

import java.util.UUID;

public interface IPacienteSyncRepository extends ISyncRepository {

    /** O paciente existe e é desta conta. */
    boolean pacienteDaConta(UUID pacienteId, long usuarioId);
}
