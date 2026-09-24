-- =====================================================================
-- TroiaFilms - V2: planos e assinaturas
-- Usuario (1) --- (N) Assinatura (N) --- (1) Plano
-- =====================================================================

CREATE TABLE plano (
    id                BIGSERIAL     PRIMARY KEY,
    nome              VARCHAR(60)   NOT NULL,
    descricao         VARCHAR(255),
    preco             NUMERIC(8, 2) NOT NULL,
    max_perfis        INTEGER       NOT NULL,
    qualidade_maxima  VARCHAR(10)   NOT NULL,
    ativo             BOOLEAN       NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_plano_nome       UNIQUE (nome),
    CONSTRAINT ck_plano_preco      CHECK (preco >= 0),
    CONSTRAINT ck_plano_max_perfis CHECK (max_perfis > 0),
    CONSTRAINT ck_plano_qualidade  CHECK (qualidade_maxima IN ('SD', 'HD', 'UHD'))
);

COMMENT ON TABLE  plano            IS 'Plano de assinatura comercializado pelo TroiaFilms';
COMMENT ON COLUMN plano.max_perfis IS 'Limite de perfis que o assinante pode criar';

CREATE TABLE assinatura (
    id          BIGSERIAL   PRIMARY KEY,
    usuario_id  BIGINT      NOT NULL,
    plano_id    BIGINT      NOT NULL,
    data_inicio DATE        NOT NULL DEFAULT CURRENT_DATE,
    data_fim    DATE,
    status      VARCHAR(20) NOT NULL DEFAULT 'ATIVA',
    CONSTRAINT fk_assinatura_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT fk_assinatura_plano   FOREIGN KEY (plano_id)   REFERENCES plano (id),
    CONSTRAINT ck_assinatura_status  CHECK (status IN ('ATIVA', 'CANCELADA', 'EXPIRADA')),
    CONSTRAINT ck_assinatura_periodo CHECK (data_fim IS NULL OR data_fim >= data_inicio)
);

CREATE INDEX ix_assinatura_usuario ON assinatura (usuario_id);

-- Regra de negocio no banco: no maximo uma assinatura ATIVA por usuario
CREATE UNIQUE INDEX uk_assinatura_ativa_por_usuario
    ON assinatura (usuario_id)
    WHERE status = 'ATIVA';

COMMENT ON TABLE  assinatura        IS 'Vinculo entre um usuario e um plano, com historico';
COMMENT ON COLUMN assinatura.status IS 'ATIVA, CANCELADA ou EXPIRADA';
