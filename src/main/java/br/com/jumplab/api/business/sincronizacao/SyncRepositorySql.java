package br.com.jumplab.api.business.sincronizacao;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** SQL comum às duas tabelas de sincronização; o nome da tabela é fixo de cada subclasse, nunca externo. */
abstract class SyncRepositorySql implements ISyncRepository {

    protected final JdbcTemplate jdbc;
    private final String tabela;
    private final boolean temPaciente;

    SyncRepositorySql(JdbcTemplate jdbc, String tabela, boolean temPaciente) {
        this.jdbc = jdbc;
        this.tabela = tabela;
        this.temPaciente = temPaciente;
    }

    @Override
    public boolean gravarSeMaisRecente(
        long usuarioId, UUID id, UUID pacienteId, Instant atualizadoEm, String conteudo) {
        String colunaPaciente = temPaciente ? ", paciente_id" : "";
        String valorPaciente = temPaciente ? ", ?" : "";
        String atualizaPaciente = temPaciente ? ", paciente_id = EXCLUDED.paciente_id" : "";
        String sql = """
            INSERT INTO %1$s (id, usuario_id%2$s, atualizado_em, conteudo, versao, recebido_em)
            VALUES (?, ?%3$s, ?, ?::jsonb, nextval('sync_versao_seq'), now())
            ON CONFLICT (id) DO UPDATE
               SET atualizado_em = EXCLUDED.atualizado_em, conteudo = EXCLUDED.conteudo,
                   versao = EXCLUDED.versao, recebido_em = EXCLUDED.recebido_em%4$s
             WHERE %1$s.usuario_id = EXCLUDED.usuario_id
               AND EXCLUDED.atualizado_em > %1$s.atualizado_em
            RETURNING id
            """.formatted(tabela, colunaPaciente, valorPaciente, atualizaPaciente);
        Timestamp quando = Timestamp.from(atualizadoEm);
        List<UUID> gravados = temPaciente
            ? jdbc.queryForList(sql, UUID.class, id, usuarioId, pacienteId, quando, conteudo)
            : jdbc.queryForList(sql, UUID.class, id, usuarioId, quando, conteudo);
        return !gravados.isEmpty();
    }

    @Override
    public boolean pertenceAOutroUsuario(UUID id, long usuarioId) {
        Integer total = jdbc.queryForObject(
            "SELECT COUNT(*) FROM " + tabela + " WHERE id = ? AND usuario_id <> ?", Integer.class, id, usuarioId);
        return total != null && total > 0;
    }

    @Override
    public List<RegistroSyncModel> listarDesde(long usuarioId, long desde, int limite) {
        String colunaPaciente = temPaciente ? "paciente_id" : "NULL::uuid AS paciente_id";
        return jdbc.query(
            "SELECT id, " + colunaPaciente + ", atualizado_em, conteudo::text AS conteudo, versao FROM "
                + tabela + " WHERE usuario_id = ? AND versao > ? ORDER BY versao LIMIT ?",
            LINHA, usuarioId, desde, limite);
    }

    private static final RowMapper<RegistroSyncModel> LINHA = (rs, n) -> new RegistroSyncModel(
        rs.getObject("id", UUID.class),
        rs.getObject("paciente_id", UUID.class),
        rs.getTimestamp("atualizado_em").toInstant(),
        rs.getString("conteudo"),
        rs.getLong("versao"));
}
