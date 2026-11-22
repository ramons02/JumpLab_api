package br.com.jumplab.api.business.sincronizacao;

import java.util.Optional;

public interface ISincronizacaoValidation {

    /** Recusa o lote inteiro (400) acima do limite de registros. */
    void validarLote(LoteEnvioDTO lote);

    /** Motivo da recusa do registro, ou vazio se ele pode ser gravado. */
    Optional<String> motivoDaRecusa(String tipo, RegistroSyncDTO registro);
}
