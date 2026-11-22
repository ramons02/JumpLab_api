package br.com.jumplab.api.business.sincronizacao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class PacienteSyncRepositoryImpl extends SyncRepositorySql implements IPacienteSyncRepository {

    public PacienteSyncRepositoryImpl(JdbcTemplate jdbc) {
        super(jdbc, "paciente_sync", false);
    }

    @Override
    public boolean pacienteDaConta(UUID pacienteId, long usuarioId) {
        Integer total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM paciente_sync WHERE id = ? AND usuario_id = ?", Integer.class, pacienteId, usuarioId);
        return total != null && total > 0;
    }
}
