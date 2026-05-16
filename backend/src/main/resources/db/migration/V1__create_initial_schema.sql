-- V1: Schema inicial — usuários, perfis antropométricos e planos de dieta.

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    name          VARCHAR(120) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE profiles (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT           NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    weight_kg            DOUBLE PRECISION NOT NULL,
    height_cm            DOUBLE PRECISION NOT NULL,
    age                  INTEGER          NOT NULL,
    sex                  VARCHAR(20)      NOT NULL,
    activity_level       VARCHAR(20)      NOT NULL,
    goal                 VARCHAR(20)      NOT NULL,
    dietary_restrictions VARCHAR(1000),
    meals_per_day        INTEGER          NOT NULL,
    body_fat_percent     DOUBLE PRECISION,
    CONSTRAINT chk_profiles_weight_positive    CHECK (weight_kg > 0),
    CONSTRAINT chk_profiles_height_positive    CHECK (height_cm > 0),
    CONSTRAINT chk_profiles_age_positive       CHECK (age > 0 AND age <= 120),
    CONSTRAINT chk_profiles_meals_range        CHECK (meals_per_day BETWEEN 1 AND 8),
    CONSTRAINT chk_profiles_sex_valid          CHECK (sex IN ('MALE', 'FEMALE')),
    CONSTRAINT chk_profiles_activity_valid     CHECK (activity_level IN ('SEDENTARY', 'LIGHT', 'MODERATE', 'ACTIVE', 'VERY_ACTIVE')),
    CONSTRAINT chk_profiles_goal_valid         CHECK (goal IN ('LOSE_WEIGHT', 'MAINTAIN', 'GAIN_MUSCLE')),
    CONSTRAINT chk_profiles_body_fat_range     CHECK (body_fat_percent IS NULL OR (body_fat_percent >= 0 AND body_fat_percent < 100))
);

CREATE TABLE diet_plans (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    tmb             INTEGER      NOT NULL,
    tdee            INTEGER      NOT NULL,
    target_calories INTEGER      NOT NULL,
    formula_used    VARCHAR(30)  NOT NULL,
    content         JSONB        NOT NULL,
    prompt_used     TEXT,
    CONSTRAINT chk_diet_plans_formula_valid CHECK (formula_used IN ('HARRIS_BENEDICT', 'MIFFLIN_ST_JEOR', 'KATCH_MCARDLE'))
);

CREATE INDEX idx_diet_plans_user_id_created_at ON diet_plans (user_id, created_at DESC);
