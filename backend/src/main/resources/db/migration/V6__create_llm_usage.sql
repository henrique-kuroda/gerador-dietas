-- V6: Registro unificado das chamadas pagas à LLM (geração e ajuste).
-- Substitui a contagem de diet_plans como base do limite diário: o ajuste custa
-- o mesmo que a geração e precisa entrar na mesma cota.

CREATE TABLE llm_usage (
    id         BIGSERIAL   PRIMARY KEY,
    user_id    BIGINT      NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    kind       VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_llm_usage_kind_valid CHECK (kind IN ('GENERATE', 'ADJUST'))
);

CREATE INDEX idx_llm_usage_user_id_created_at ON llm_usage (user_id, created_at DESC);

-- Backfill: cada plano existente representa uma geração já cobrada. Mantém o
-- limite honesto para quem gerou dietas nas últimas 24h antes desta migration.
INSERT INTO llm_usage (user_id, kind, created_at)
SELECT user_id, 'GENERATE', created_at FROM diet_plans;
