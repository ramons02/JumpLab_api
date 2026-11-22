package br.com.jumplab.api.business.recuperacao;

import br.com.jumplab.api.core.repositories.AbstractRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Date;
import java.util.List;

@org.springframework.stereotype.Repository
public class SenhaTemporariaRepositoryImpl extends AbstractRepository<SenhaTemporariaModel>
    implements ISenhaTemporariaRepository {

    private static final String INSERIR = """
        INSERT INTO senha_temporaria (usuario_id, pedido_id, senha_hash, gerada_por, gerada_em, expira_em)
        VALUES (?, ?, ?, ?, ?, ?)
        """;
    private static final String VALIDAS = """
         WHERE usuario_id = ? AND usada_em IS NULL AND invalidada_em IS NULL AND expira_em > now() AND ativo
         ORDER BY gerada_em DESC
        """;
    private static final String MARCAR_USADA = """
        UPDATE senha_temporaria SET usada_em = now() WHERE id = ? AND usada_em IS NULL
        """;
    private static final String INVALIDAR_ABERTAS = """
        UPDATE senha_temporaria SET invalidada_em = now()
        WHERE usuario_id = ? AND usada_em IS NULL AND invalidada_em IS NULL
        """;
    private static final String ATUALIZAR = """
        UPDATE senha_temporaria SET usada_em = ?, invalidada_em = ? WHERE id = ?
        """;

    public SenhaTemporariaRepositoryImpl(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    protected String getTableName() {
        return "senha_temporaria";
    }

    @Override
    protected RowMapper<SenhaTemporariaModel> getRowMapper() {
        return (rs, linha) -> {
            SenhaTemporariaModel senha = new SenhaTemporariaModel();
            senha.setId(rs.getLong("id"));
            senha.setUsuarioId(rs.getLong("usuario_id"));
            senha.setPedidoId(rs.getLong("pedido_id"));
            senha.setSenhaHash(rs.getString("senha_hash"));
            senha.setGeradaPor(rs.getLong("gerada_por"));
            senha.setGeradaEm(rs.getTimestamp("gerada_em").toInstant());
            senha.setExpiraEm(rs.getTimestamp("expira_em").toInstant());
            senha.setUsadaEm(instante(rs.getTimestamp("usada_em")));
            senha.setInvalidadaEm(instante(rs.getTimestamp("invalidada_em")));
            senha.setDataHoraCriacao(new Date(rs.getTimestamp("data_hora_criacao").getTime()));
            senha.setAtivo(rs.getBoolean("ativo"));
            return senha;
        };
    }

    @Override
    public List<SenhaTemporariaModel> listarValidasDoUsuario(Long usuarioId) {
        return jdbc.query(getSelectSql() + VALIDAS, getRowMapper(), usuarioId);
    }

    @Override
    public boolean marcarUsada(Long id) {
        return jdbc.update(MARCAR_USADA, id) == 1;
    }

    @Override
    public void invalidarAbertasDoUsuario(Long usuarioId) {
        jdbc.update(INVALIDAR_ABERTAS, usuarioId);
    }

    @Override
    public SenhaTemporariaModel salvar(SenhaTemporariaModel senha) {
        KeyHolder chave = new GeneratedKeyHolder();
        jdbc.update(conexao -> {
            PreparedStatement comando = conexao.prepareStatement(INSERIR, new String[] {"id"});
            comando.setLong(1, senha.getUsuarioId());
            comando.setLong(2, senha.getPedidoId());
            comando.setString(3, senha.getSenhaHash());
            comando.setLong(4, senha.getGeradaPor());
            comando.setTimestamp(5, Timestamp.from(senha.getGeradaEm()));
            comando.setTimestamp(6, Timestamp.from(senha.getExpiraEm()));
            return comando;
        }, chave);
        return buscarPorId(chave.getKeyAs(Long.class)).orElseThrow();
    }

    @Override
    public void atualizar(SenhaTemporariaModel senha) {
        jdbc.update(ATUALIZAR, timestamp(senha.getUsadaEm()), timestamp(senha.getInvalidadaEm()), senha.getId());
    }

    private static Instant instante(Timestamp valor) {
        return valor == null ? null : valor.toInstant();
    }

    private static Timestamp timestamp(Instant valor) {
        return valor == null ? null : Timestamp.from(valor);
    }
}
