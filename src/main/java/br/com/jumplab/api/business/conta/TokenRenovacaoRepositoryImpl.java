package br.com.jumplab.api.business.conta;

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
import java.util.Optional;

@org.springframework.stereotype.Repository
public class TokenRenovacaoRepositoryImpl extends AbstractRepository<TokenRenovacaoModel>
    implements ITokenRenovacaoRepository {

    private static final String INSERIR = """
        INSERT INTO token_renovacao (usuario_id, token_hash, emitido_em, expira_em, troca_senha_pendente)
        VALUES (?, ?, ?, ?, ?)
        """;
    private static final String ATUALIZAR = """
        UPDATE token_renovacao SET usado_em = ?, revogado_em = ?
        WHERE id = ?
        """;
    private static final String VALIDO = """
         WHERE token_hash = ? AND usado_em IS NULL AND revogado_em IS NULL AND expira_em > now() AND ativo
        """;
    private static final String MARCAR_USADO = """
        UPDATE token_renovacao SET usado_em = now()
        WHERE id = ? AND usado_em IS NULL
        """;
    private static final String REVOGAR = """
        UPDATE token_renovacao SET revogado_em = now()
        WHERE token_hash = ? AND revogado_em IS NULL
        """;

    private static final String REVOGAR_PENDENTES = """
        UPDATE token_renovacao SET revogado_em = now()
        WHERE usuario_id = ? AND troca_senha_pendente AND revogado_em IS NULL
        """;

    public TokenRenovacaoRepositoryImpl(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    protected String getTableName() {
        return "token_renovacao";
    }

    @Override
    protected RowMapper<TokenRenovacaoModel> getRowMapper() {
        return (rs, linha) -> {
            TokenRenovacaoModel token = new TokenRenovacaoModel();
            token.setId(rs.getLong("id"));
            token.setUsuarioId(rs.getLong("usuario_id"));
            token.setTokenHash(rs.getString("token_hash"));
            token.setEmitidoEm(instante(rs.getTimestamp("emitido_em")));
            token.setExpiraEm(instante(rs.getTimestamp("expira_em")));
            token.setUsadoEm(instante(rs.getTimestamp("usado_em")));
            token.setRevogadoEm(instante(rs.getTimestamp("revogado_em")));
            token.setTrocaSenhaPendente(rs.getBoolean("troca_senha_pendente"));
            token.setDataHoraCriacao(new Date(rs.getTimestamp("data_hora_criacao").getTime()));
            token.setAtivo(rs.getBoolean("ativo"));
            return token;
        };
    }

    @Override
    public Optional<TokenRenovacaoModel> buscarValidoPorHash(String tokenHash) {
        List<TokenRenovacaoModel> encontrados = jdbc.query(getSelectSql() + VALIDO, getRowMapper(), tokenHash);
        return encontrados.stream().findFirst();
    }

    @Override
    public boolean marcarUsado(Long id) {
        return jdbc.update(MARCAR_USADO, id) == 1;
    }

    @Override
    public void revogar(String tokenHash) {
        jdbc.update(REVOGAR, tokenHash);
    }

    @Override
    public void revogarPendentesDoUsuario(Long usuarioId) {
        jdbc.update(REVOGAR_PENDENTES, usuarioId);
    }

    @Override
    public TokenRenovacaoModel salvar(TokenRenovacaoModel token) {
        KeyHolder chave = new GeneratedKeyHolder();
        jdbc.update(conexao -> {
            PreparedStatement comando = conexao.prepareStatement(INSERIR, new String[] {"id"});
            comando.setLong(1, token.getUsuarioId());
            comando.setString(2, token.getTokenHash());
            comando.setTimestamp(3, Timestamp.from(token.getEmitidoEm()));
            comando.setTimestamp(4, Timestamp.from(token.getExpiraEm()));
            comando.setBoolean(5, token.isTrocaSenhaPendente());
            return comando;
        }, chave);
        return buscarPorId(chave.getKeyAs(Long.class)).orElseThrow();
    }

    @Override
    public void atualizar(TokenRenovacaoModel token) {
        jdbc.update(ATUALIZAR, timestamp(token.getUsadoEm()), timestamp(token.getRevogadoEm()), token.getId());
    }

    private static Instant instante(Timestamp valor) {
        return valor == null ? null : valor.toInstant();
    }

    private static Timestamp timestamp(Instant valor) {
        return valor == null ? null : Timestamp.from(valor);
    }
}
