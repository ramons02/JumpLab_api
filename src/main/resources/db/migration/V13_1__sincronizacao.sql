-- HU013: sincronização de pacientes e avaliações (backup e troca de aparelho).
-- O conteúdo é guardado como o app enviou (JSONB); nenhum vídeo sobe (D14).
-- Conflito: vence a alteração mais recente (D15), pela coluna atualizado_em.

CREATE SEQUENCE sync_versao_seq;

CREATE TABLE paciente_sync (
    id            UUID PRIMARY KEY,
    usuario_id    BIGINT      NOT NULL REFERENCES usuario (id),
    atualizado_em TIMESTAMPTZ NOT NULL,
    conteudo      JSONB       NOT NULL,
    versao        BIGINT      NOT NULL,
    recebido_em   TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_paciente_sync_usuario_versao ON paciente_sync (usuario_id, versao);

CREATE TABLE avaliacao_sync (
    id            UUID PRIMARY KEY,
    usuario_id    BIGINT      NOT NULL REFERENCES usuario (id),
    paciente_id   UUID        NOT NULL REFERENCES paciente_sync (id),
    atualizado_em TIMESTAMPTZ NOT NULL,
    conteudo      JSONB       NOT NULL,
    versao        BIGINT      NOT NULL,
    recebido_em   TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_avaliacao_sync_usuario_versao ON avaliacao_sync (usuario_id, versao);
