package br.com.jumplab.api.core.helpers;

import br.com.jumplab.api.core.domains.BaseModel;
import br.com.jumplab.api.core.dtos.BaseDTO;

public interface IGenericMapper<T extends BaseModel, D extends BaseDTO> {
    D toDto(T entity);
    T toEntity(D dto);
}
