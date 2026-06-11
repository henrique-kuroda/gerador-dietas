-- V3: Snapshot do perfil no momento da geração da dieta.
-- O PDF e o detalhe da dieta passam a refletir o perfil da época, não o atual.
-- Colunas anuláveis: planos antigos não têm snapshot (fallback para o perfil atual).

ALTER TABLE diet_plans
    ADD COLUMN profile_weight_kg            DOUBLE PRECISION,
    ADD COLUMN profile_height_cm            DOUBLE PRECISION,
    ADD COLUMN profile_age                  INTEGER,
    ADD COLUMN profile_sex                  VARCHAR(20),
    ADD COLUMN profile_activity_level       VARCHAR(20),
    ADD COLUMN profile_goal                 VARCHAR(20),
    ADD COLUMN profile_dietary_restrictions VARCHAR(1000),
    ADD COLUMN profile_meals_per_day        INTEGER,
    ADD COLUMN profile_body_fat_percent     DOUBLE PRECISION;
