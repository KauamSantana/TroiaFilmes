-- =====================================================================
-- TroiaFilmes - V1: tabelas do nucleo do dominio
-- Usuario -> Perfil -> Avaliacao <- Filme <-> Categoria
-- =====================================================================

CREATE TABLE usuario (
    id        BIGSERIAL    PRIMARY KEY,
    nome      VARCHAR(120) NOT NULL,
    email     VARCHAR(160) NOT NULL,
    senha     VARCHAR(100) NOT NULL,
    role      VARCHAR(20)  NOT NULL DEFAULT 'USER',
    ativo     BOOLEAN      NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_role  CHECK (role IN ('ADMIN', 'USER'))
);

COMMENT ON TABLE  usuario       IS 'Conta de acesso ao TroiaFilmes';
COMMENT ON COLUMN usuario.senha IS 'Hash BCrypt - a senha em texto puro nunca e persistida';
COMMENT ON COLUMN usuario.ativo IS 'Soft delete: usuario inativo nao consegue autenticar';

-- Um usuario possui varios perfis (1:N)
CREATE TABLE perfil (
    id         BIGSERIAL   PRIMARY KEY,
    usuario_id BIGINT      NOT NULL,
    nome       VARCHAR(60) NOT NULL,
    avatar_url VARCHAR(500),
    infantil   BOOLEAN     NOT NULL DEFAULT FALSE,
    criado_em  TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_perfil_usuario     FOREIGN KEY (usuario_id) REFERENCES usuario (id) ON DELETE CASCADE,
    CONSTRAINT uk_perfil_usuario_nome UNIQUE (usuario_id, nome)
);

CREATE INDEX ix_perfil_usuario ON perfil (usuario_id);

COMMENT ON TABLE  perfil          IS 'Perfil de consumo dentro de uma conta (estilo Netflix)';
COMMENT ON COLUMN perfil.infantil IS 'Marca perfil infantil';

CREATE TABLE filme (
    id              BIGSERIAL    PRIMARY KEY,
    titulo          VARCHAR(180) NOT NULL,
    sinopse         TEXT,
    ano_lancamento  INTEGER      NOT NULL,
    duracao_minutos INTEGER      NOT NULL,
    poster_url      VARCHAR(500),
    criado_em       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_filme_ano     CHECK (ano_lancamento BETWEEN 1888 AND 2200),
    CONSTRAINT ck_filme_duracao CHECK (duracao_minutos > 0)
);

CREATE INDEX ix_filme_titulo ON filme (LOWER(titulo));
CREATE INDEX ix_filme_ano    ON filme (ano_lancamento);

COMMENT ON TABLE filme IS 'Titulo disponivel no catalogo';

CREATE TABLE categoria (
    id        BIGSERIAL   PRIMARY KEY,
    nome      VARCHAR(80) NOT NULL,
    descricao VARCHAR(255),
    CONSTRAINT uk_categoria_nome UNIQUE (nome)
);

COMMENT ON TABLE categoria IS 'Genero / categoria de filme';

-- Filme N:N Categoria, resolvido pela tabela associativa
CREATE TABLE filme_categoria (
    filme_id     BIGINT NOT NULL,
    categoria_id BIGINT NOT NULL,
    CONSTRAINT pk_filme_categoria PRIMARY KEY (filme_id, categoria_id),
    CONSTRAINT fk_fc_filme     FOREIGN KEY (filme_id)     REFERENCES filme (id)     ON DELETE CASCADE,
    CONSTRAINT fk_fc_categoria FOREIGN KEY (categoria_id) REFERENCES categoria (id) ON DELETE CASCADE
);

CREATE INDEX ix_fc_categoria ON filme_categoria (categoria_id);

COMMENT ON TABLE filme_categoria IS 'Tabela associativa do relacionamento N:N entre filme e categoria';

-- Um perfil avalia um filme uma unica vez
CREATE TABLE avaliacao (
    id         BIGSERIAL PRIMARY KEY,
    perfil_id  BIGINT    NOT NULL,
    filme_id   BIGINT    NOT NULL,
    nota       INTEGER   NOT NULL,
    comentario TEXT,
    criado_em  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_avaliacao_perfil      FOREIGN KEY (perfil_id) REFERENCES perfil (id) ON DELETE CASCADE,
    CONSTRAINT fk_avaliacao_filme       FOREIGN KEY (filme_id)  REFERENCES filme (id)  ON DELETE CASCADE,
    CONSTRAINT uk_avaliacao_perfil_filme UNIQUE (perfil_id, filme_id),
    CONSTRAINT ck_avaliacao_nota        CHECK (nota BETWEEN 1 AND 5)
);

CREATE INDEX ix_avaliacao_filme ON avaliacao (filme_id);

COMMENT ON TABLE  avaliacao      IS 'Nota e comentario de um perfil sobre um filme';
COMMENT ON COLUMN avaliacao.nota IS 'Escala de 1 a 5';
