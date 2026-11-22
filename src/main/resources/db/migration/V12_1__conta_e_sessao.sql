CREATE TABLE usuario (
    id                BIGSERIAL    PRIMARY KEY,
    nome              VARCHAR(120) NOT NULL,
    email             VARCHAR(254) NOT NULL,
    senha_hash        VARCHAR(100) NOT NULL,
    admin             BOOLEAN      NOT NULL DEFAULT false,
    data_hora_criacao TIMESTAMPTZ  NOT NULL DEFAULT now(),
    ativo             BOOLEAN      NOT NULL DEFAULT true,
    CONSTRAINT ck_usuario_email_normalizado CHECK (email = lower(trim(email)))
);

CREATE UNIQUE INDEX ux_usuario_email ON usuario (email);

CREATE TABLE token_renovacao (
    id                BIGSERIAL   PRIMARY KEY,
    usuario_id        BIGINT      NOT NULL REFERENCES usuario (id),
    token_hash        CHAR(64)    NOT NULL,
    emitido_em        TIMESTAMPTZ NOT NULL,
    expira_em         TIMESTAMPTZ NOT NULL,
    usado_em          TIMESTAMPTZ,
    revogado_em       TIMESTAMPTZ,
    data_hora_criacao TIMESTAMPTZ NOT NULL DEFAULT now(),
    ativo             BOOLEAN     NOT NULL DEFAULT true
);

CREATE UNIQUE INDEX ux_token_renovacao_hash ON token_renovacao (token_hash);
CREATE INDEX ix_token_renovacao_usuario ON token_renovacao (usuario_id);
