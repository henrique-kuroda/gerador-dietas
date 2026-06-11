-- V2: Expande o enum Goal para incluir AGGRESSIVE_LOSS (-30%) e AGGRESSIVE_GAIN (+20%).

ALTER TABLE profiles DROP CONSTRAINT chk_profiles_goal_valid;

ALTER TABLE profiles ADD CONSTRAINT chk_profiles_goal_valid
    CHECK (goal IN ('AGGRESSIVE_LOSS', 'LOSE_WEIGHT', 'MAINTAIN', 'GAIN_MUSCLE', 'AGGRESSIVE_GAIN'));
