-- V5: Ajuste conversacional do plano (sobrescreve o plano no lugar).
-- Campos opcionais; planos existentes seguem válidos (adjustment_count = 0).

ALTER TABLE diet_plans
    ADD COLUMN adjusted_at      TIMESTAMPTZ,
    ADD COLUMN adjustment_count INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN last_adjustment  VARCHAR(500);

ALTER TABLE diet_plans
    ADD CONSTRAINT chk_diet_plans_adjustment_count_range
        CHECK (adjustment_count >= 0 AND adjustment_count <= 50);
