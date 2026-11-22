package br.com.jumplab.api.core.repositories;

import br.com.jumplab.api.core.domains.BaseModel;

import java.util.List;
import java.util.Optional;

public interface Repository<T extends BaseModel> {
    Optional<T> buscarPorId(Long id);
    List<T> listarTodos();
    List<T> listarTodosIncluindoInativos();
    T salvar(T entity);
    void atualizar(T entity);
    void deletar(Long id);
    void toggleStatus(Long id);
}
