package br.com.jumplab.api.business.sincronizacao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class AvaliacaoSyncRepositoryImpl extends SyncRepositorySql implements IAvaliacaoSyncRepository {

    public AvaliacaoSyncRepositoryImpl(JdbcTemplate jdbc) {
        super(jdbc, "avaliacao_sync", true);
    }
}
