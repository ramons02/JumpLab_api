package br.com.jumplab.api.core.repositories;

import br.com.jumplab.api.core.domains.BaseModel;

public interface IGenericRepository<T extends BaseModel> extends Repository<T> {}
