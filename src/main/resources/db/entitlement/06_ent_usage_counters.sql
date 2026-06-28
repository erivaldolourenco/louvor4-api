-- Tabela: ent_usage_counters
-- Contador de uso para QUOTAs — reseta mensalmente via period_start
-- subscription_id é UUID simples (sem FK) para evitar joins

CREATE TABLE IF NOT EXISTS ent_usage_counters (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID         NOT NULL,
    entitlement_key VARCHAR(100) NOT NULL,
    count           INTEGER      NOT NULL DEFAULT 0,
    period_start    DATE         NOT NULL,

    CONSTRAINT uq_ent_usage_counter UNIQUE (subscription_id, entitlement_key, period_start)
);
