package br.com.jumplab.api.business.sincronizacao;

import java.util.List;

/** Corpo de {@code POST /api/sync/enviar}: até 100 registros somando as duas listas. */
public record LoteEnvioDTO(List<RegistroSyncDTO> pacientes, List<RegistroSyncDTO> avaliacoes) {

    public List<RegistroSyncDTO> pacientesOuVazio() {
        return pacientes == null ? List.of() : pacientes;
    }

    public List<RegistroSyncDTO> avaliacoesOuVazio() {
        return avaliacoes == null ? List.of() : avaliacoes;
    }
}
