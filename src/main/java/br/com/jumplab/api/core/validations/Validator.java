package br.com.jumplab.api.core.validations;

import br.com.jumplab.api.core.exceptions.NegocioException;

public interface Validator<T> {
    void validar(T entity) throws NegocioException;
}
