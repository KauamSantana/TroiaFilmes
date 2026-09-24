-- =====================================================================
-- TroiaFilms - V3: Minha Lista e Historico de Visualizacao
-- Perfil (N) --- (N) Filme, resolvido por entidades associativas
-- =====================================================================

-- "Minha Lista": N:N entre perfil e filme, com atributo proprio
CREATE TABLE item_lista (
    id           BIGSERIAL PRIMARY KEY,
    perfil_id    BIGINT    NOT NULL,
    filme_id     BIGINT    NOT NULL,
    adicionado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_item_lista_perfil       FOREIGN KEY (perfil_id) REFERENCES perfil (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_lista_filme        FOREIGN KEY (filme_id)  REFERENCES filme (id)  ON DELETE CASCADE,
    CONSTRAINT uk_item_lista_perfil_filme UNIQUE (perfil_id, filme_id)
);

CREATE INDEX ix_item_lista_perfil ON item_lista (perfil_id);

COMMENT ON TABLE item_lista IS 'Minha Lista: filmes salvos por um perfil para assistir depois';

CREATE TABLE historico_visualizacao (
    id                BIGSERIAL PRIMARY KEY,
    perfil_id         BIGINT    NOT NULL,
    filme_id          BIGINT    NOT NULL,
    minutos_assistidos INTEGER  NOT NULL DEFAULT 0,
    concluido         BOOLEAN   NOT NULL DEFAULT FALSE,
    assistido_em      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historico_perfil       FOREIGN KEY (perfil_id) REFERENCES perfil (id) ON DELETE CASCADE,
    CONSTRAINT fk_historico_filme        FOREIGN KEY (filme_id)  REFERENCES filme (id)  ON DELETE CASCADE,
    CONSTRAINT uk_historico_perfil_filme UNIQUE (perfil_id, filme_id),
    CONSTRAINT ck_historico_minutos      CHECK (minutos_assistidos >= 0)
);

CREATE INDEX ix_historico_perfil ON historico_visualizacao (perfil_id, assistido_em DESC);

COMMENT ON TABLE  historico_visualizacao                    IS 'Progresso de visualizacao usado pelo "Continuar assistindo"';
COMMENT ON COLUMN historico_visualizacao.minutos_assistidos IS 'Posicao atual de reproducao, em minutos';
COMMENT ON COLUMN historico_visualizacao.concluido          IS 'Marcado quando o progresso atinge 90% da duracao do filme';
