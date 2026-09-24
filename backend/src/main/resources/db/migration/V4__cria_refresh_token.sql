-- =====================================================================
-- TroiaFilmes - V4: refresh tokens
-- Persistidos para permitir revogacao no logout
-- =====================================================================

CREATE TABLE refresh_token (
    id         BIGSERIAL    PRIMARY KEY,
    usuario_id BIGINT       NOT NULL,
    token      VARCHAR(255) NOT NULL,
    expira_em  TIMESTAMP    NOT NULL,
    revogado   BOOLEAN      NOT NULL DEFAULT FALSE,
    criado_em  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refresh_token_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT uk_refresh_token_token   UNIQUE (token)
);

CREATE INDEX ix_refresh_token_usuario ON refresh_token (usuario_id);

COMMENT ON TABLE refresh_token IS 'Refresh tokens emitidos no login; revogados no logout';
