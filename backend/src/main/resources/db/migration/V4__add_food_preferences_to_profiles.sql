-- V4: Preferências alimentares estruturadas (item 4.1 do plano de melhorias).
-- Todas as colunas são opcionais — perfis existentes seguem válidos.

ALTER TABLE profiles
    ADD COLUMN favorite_foods    VARCHAR(1000),
    ADD COLUMN disliked_foods    VARCHAR(1000),
    ADD COLUMN budget            VARCHAR(20),
    ADD COLUMN region            VARCHAR(20),
    ADD COLUMN max_prep_minutes  INTEGER,
    ADD COLUMN eats_out_at_lunch BOOLEAN;

ALTER TABLE profiles
    ADD CONSTRAINT chk_profiles_budget_valid
        CHECK (budget IS NULL OR budget IN ('ECONOMICAL', 'MODERATE', 'UNRESTRICTED')),
    ADD CONSTRAINT chk_profiles_region_valid
        CHECK (region IS NULL OR region IN ('NORTE', 'NORDESTE', 'CENTRO_OESTE', 'SUDESTE', 'SUL')),
    ADD CONSTRAINT chk_profiles_max_prep_range
        CHECK (max_prep_minutes IS NULL OR (max_prep_minutes > 0 AND max_prep_minutes <= 480));
