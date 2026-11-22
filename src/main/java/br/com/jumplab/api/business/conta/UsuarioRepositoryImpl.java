package br.com.jumplab.api.business.conta;

import br.com.jumplab.api.core.repositories.AbstractRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;

import java.sql.PreparedStatement;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@org.springframework.stereotype.Repository
public class UsuarioRepositoryImpl extends AbstractRepository<UsuarioModel> implements IUsuarioRepository {

    private static final String INSERIR = """
        INSERT INTO usuario (nome, email, senha_hash, admin)
        VALUES (?, ?, ?, ?)
        """;
    private static final String ATUALIZAR = """
        UPDATE usuario SET nome = ?, email = ?, senha_hash = ?, admin = ?
        WHERE id = ?
        """;

    public UsuarioRepositoryImpl(JdbcTemplate jdbc) {
        super(jdbc);
    }

    @Override
    protected String getTableName() {
        return "usuario";
    }

    @Override
    protected RowMapper<UsuarioModel> getRowMapper() {
        return (rs, linha) -> {
            UsuarioModel usuario = new UsuarioModel();
            usuario.setId(rs.getLong("id"));
            usuario.setNome(rs.getString("nome"));
            usuario.setEmail(rs.getString("email"));
            usuario.setSenhaHash(rs.getString("senha_hash"));
            usuario.setAdmin(rs.getBoolean("admin"));
            usuario.setDataHoraCriacao(new Date(rs.getTimestamp("data_hora_criacao").getTime()));
            usuario.setAtivo(rs.getBoolean("ativo"));
            return usuario;
        };
    }

    @Override
    public Optional<UsuarioModel> buscarPorEmail(String email) {
        List<UsuarioModel> encontrados = jdbc.query(getSelectSql() + " WHERE email = ?", getRowMapper(), email);
        return encontrados.stream().findFirst();
    }

    @Override
    public UsuarioModel salvar(UsuarioModel usuario) {
        KeyHolder chave = new GeneratedKeyHolder();
        jdbc.update(conexao -> {
            PreparedStatement comando = conexao.prepareStatement(INSERIR, new String[] {"id"});
            comando.setString(1, usuario.getNome());
            comando.setString(2, usuario.getEmail());
            comando.setString(3, usuario.getSenhaHash());
            comando.setBoolean(4, usuario.isAdmin());
            return comando;
        }, chave);
        return buscarPorId(chave.getKeyAs(Long.class)).orElseThrow();
    }

    @Override
    public void atualizar(UsuarioModel usuario) {
        jdbc.update(ATUALIZAR, usuario.getNome(), usuario.getEmail(), usuario.getSenhaHash(),
            usuario.isAdmin(), usuario.getId());
    }
}
