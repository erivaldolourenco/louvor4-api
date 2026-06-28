-- Tabela: ent_plans
-- Planos disponíveis no sistema (FREE, STARTER, PRO, ELITE)

CREATE TABLE IF NOT EXISTS ent_plans (
    id           UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    name         VARCHAR(50)    NOT NULL,
    display_name VARCHAR(100)   NOT NULL,
    price        NUMERIC(10, 2) NOT NULL,

    CONSTRAINT uq_ent_plans_name UNIQUE (name)
);
