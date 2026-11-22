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
public class PedidoRecuperacaoRepositoryImpl extends AbstractRepository<PedidoRecuperacaoModel>
    implements IPedidoRecuperacaoRepository {

    private static final String INSERIR = """
        INSERT INTO pedido_recuperacao (usuario_id) VALUES (?)
        """;
    private static final String ABRIR_OU_ATUALIZAR = """
        INSERT INTO pedido_recuperacao (usuario_id) VALUES (?)
        ON CONFLICT (usuario_id) WHERE status = 'aberto' DO UPDATE SET pedido_em = now()
        """;
    private static final String LISTAR_COM_EMAIL = """
        SELECT p.id, u.email, p.pedido_em, p.status, p.atendido_em
          FROM pedido_recuperacao p JOIN usuario u ON u.id = p.usuario_id
         ORDER BY p.pedido_em DESC, p.id DESC
        """;
    private static final String MARCAR_ATENDIDO = """
        UPDATE pedido_recuperacao SET status = 'atendido', atendido_em = now()
        WHERE id = ? AND status = 'aberto'
        """;
    private static final String ATUALIZAR = """
        UPDATE pedido_recuperacao SET status = ?, atendido_em = ? WHERE id = ?
        """;

    public PedidoRecuperacaoRepositoryImpl(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    protected String getTableName() {
        return "pedido_recuperacao";
    }

    @Override
    protected RowMapper<PedidoRecuperacaoModel> getRowMapper() {
        return (rs, linha) -> {
            PedidoRecuperacaoModel pedido = new PedidoRecuperacaoModel();
            pedido.setId(rs.getLong("id"));
            pedido.setUsuarioId(rs.getLong("usuario_id"));
            pedido.setPedidoEm(rs.getTimestamp("pedido_em").toInstant());
            pedido.setStatus(rs.getString("status"));
            pedido.setAtendidoEm(instante(rs.getTimestamp("atendido_em")));
            pedido.setDataHoraCriacao(new Date(rs.getTimestamp("data_hora_criacao").getTime()));
            pedido.setAtivo(rs.getBoolean("ativo"));
            return pedido;
        };
    }

    @Override
    public void abrirOuAtualizar(Long usuarioId) {
        jdbc.update(ABRIR_OU_ATUALIZAR, usuarioId);
    }

    @Override
    public List<PedidoRecuperacaoDTO> listarComEmail() {
        return jdbc.query(LISTAR_COM_EMAIL, (rs, linha) -> new PedidoRecuperacaoDTO(
            rs.getLong("id"),
            rs.getString("email"),
            rs.getTimestamp("pedido_em").toInstant(),
            rs.getString("status"),
            instante(rs.getTimestamp("atendido_em"))));
    }

    @Override
    public boolean marcarAtendido(Long id) {
        return jdbc.update(MARCAR_ATENDIDO, id) == 1;
    }

    @Override
    public PedidoRecuperacaoModel salvar(PedidoRecuperacaoModel pedido) {
        KeyHolder chave = new GeneratedKeyHolder();
        jdbc.update(conexao -> {
            PreparedStatement comando = conexao.prepareStatement(INSERIR, new String[] {"id"});
            comando.setLong(1, pedido.getUsuarioId());
            return comando;
        }, chave);
        return buscarPorId(chave.getKeyAs(Long.class)).orElseThrow();
    }

    @Override
    public void atualizar(PedidoRecuperacaoModel pedido) {
        Timestamp atendido = pedido.getAtendidoEm() == null ? null : Timestamp.from(pedido.getAtendidoEm());
        jdbc.update(ATUALIZAR, pedido.getStatus(), atendido, pedido.getId());
    }

    private static Instant instante(Timestamp valor) {
        return valor == null ? null : valor.toInstant();
    }
}
