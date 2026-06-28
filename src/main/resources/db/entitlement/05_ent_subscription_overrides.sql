-- Tabela: ent_subscription_overrides
-- Exceções por cliente aplicadas pelo suporte sem redeploy
-- subscription_id é UUID simples (sem FK) para evitar joins

CREATE TABLE IF NOT EXISTS ent_subscription_overrides (
    id              UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    subscription_id UUID         NOT NULL,
    entitlement_key VARCHAR(100) NOT NULL,
    value           VARCHAR(50)  NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    expires_at      TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_ent_overrides_lookup
    ON ent_subscription_overrides (subscription_id, entitlement_key, active);
