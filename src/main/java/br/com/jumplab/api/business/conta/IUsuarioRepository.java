package br.com.jumplab.api.business.conta;

import br.com.jumplab.api.core.repositories.IGenericRepository;

import java.util.Optional;

public interface IUsuarioRepository extends IGenericRepository<UsuarioModel> {

    /** O e-mail precisa chegar já normalizado (minúsculo, sem espaços nas pontas). */
    Optional<UsuarioModel> buscarPorEmail(String email);
}
