CREATE TABLE pedido_recuperacao (
    id                BIGSERIAL   PRIMARY KEY,
    usuario_id        BIGINT      NOT NULL REFERENCES usuario (id),
    pedido_em         TIMESTAMPTZ NOT NULL DEFAULT now(),
    status            VARCHAR(10) NOT NULL DEFAULT 'aberto',
    atendido_em       TIMESTAMPTZ,
    data_hora_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    ativo             BOOLEAN     NOT NULL DEFAULT true,
    CONSTRAINT ck_pedido_status CHECK (status IN ('aberto', 'atendido')),
    CONSTRAINT ck_pedido_atendido CHECK (status = 'aberto' OR atendido_em IS NOT NULL)
);

CREATE UNIQUE INDEX ux_pedido_aberto_por_usuario ON pedido_recuperacao (usuario_id) WHERE status = 'aberto';

CREATE TABLE senha_temporaria (
    id                BIGSERIAL    PRIMARY KEY,
    usuario_id        BIGINT       NOT NULL REFERENCES usuario (id),
    pedido_id         BIGINT       NOT NULL REFERENCES pedido_recuperacao (id),
    senha_hash        VARCHAR(100) NOT NULL,
    gerada_por        BIGINT       NOT NULL REFERENCES usuario (id),
    gerada_em         TIMESTAMPTZ  NOT NULL,
    expira_em         TIMESTAMPTZ  NOT NULL,
    usada_em          TIMESTAMPTZ,
    invalidada_em     TIMESTAMPTZ,
    data_hora_criacao TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ativo             BOOLEAN      NOT NULL DEFAULT true
);

CREATE INDEX ix_senha_temporaria_usuario ON senha_temporaria (usuario_id);

ALTER TABLE token_renovacao ADD COLUMN troca_senha_pendente BOOLEAN NOT NULL DEFAULT false;
