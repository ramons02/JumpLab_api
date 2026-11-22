package br.com.jumplab.api.business.conta;

import br.com.jumplab.api.core.AbstractIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UsuarioRepositoryIT extends AbstractIntegrationTest {

    @Autowired
    private IUsuarioRepository repositorio;

    @BeforeEach
    void limpar() {
        jdbc.update("TRUNCATE token_renovacao, usuario RESTART IDENTITY CASCADE");
    }

    @Test
    void salvaEBuscaPorEmail() {
        UsuarioModel salvo = repositorio.salvar(usuario("ana@exemplo.com"));

        assertNotNull(salvo.getId());
        assertTrue(salvo.isAtivo());
        assertFalse(salvo.isAdmin());
        assertNotNull(salvo.getDataHoraCriacao());
        UsuarioModel lido = repositorio.buscarPorEmail("ana@exemplo.com").orElseThrow();
        assertEquals("Ana Souza", lido.getNome());
        assertEquals("hash", lido.getSenhaHash());
    }

    @Test
    void emailInexistenteNaoAcha() {
        assertTrue(repositorio.buscarPorEmail("ninguem@exemplo.com").isEmpty());
    }

    @Test
    void indiceUnicoRecusaEmailRepetido() {
        repositorio.salvar(usuario("ana@exemplo.com"));

        assertThrows(DataIntegrityViolationException.class, () -> repositorio.salvar(usuario("ana@exemplo.com")));
    }

    @Test
    void checkRecusaEmailNaoNormalizado() {
        assertThrows(DataIntegrityViolationException.class, () -> repositorio.salvar(usuario("Ana@Exemplo.com")));
        assertThrows(DataIntegrityViolationException.class, () -> repositorio.salvar(usuario(" ana@exemplo.com")));
    }

    private static UsuarioModel usuario(String email) {
        UsuarioModel u = new UsuarioModel();
        u.setNome("Ana Souza");
        u.setEmail(email);
        u.setSenhaHash("hash");
        return u;
    }
}
