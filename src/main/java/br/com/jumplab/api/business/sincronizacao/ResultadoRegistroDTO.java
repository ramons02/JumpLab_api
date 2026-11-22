package br.com.jumplab.api.business.sincronizacao;

/** Situação de cada registro enviado: aceito, ignorado (nada mudou) ou recusado (com o motivo). */
public record ResultadoRegistroDTO(String tipo, String id, String situacao, String motivo) {

    static final String ACEITO = "aceito";
    static final String IGNORADO = "ignorado";
    static final String RECUSADO = "recusado";
    static final String PACIENTE = "paciente";
    static final String AVALIACAO = "avaliacao";
}
