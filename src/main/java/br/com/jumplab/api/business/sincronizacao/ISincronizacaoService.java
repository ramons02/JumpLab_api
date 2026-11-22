package br.com.jumplab.api.business.sincronizacao;

public interface ISincronizacaoService {

    ResultadoEnvioDTO enviar(long usuarioId, LoteEnvioDTO lote);

    PaginaRecebimentoDTO receber(long usuarioId, long desde, int limite);
}
