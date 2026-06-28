-- Tabela: ent_subscriptions
-- Assinatura ativa de cada usuário

CREATE TABLE IF NOT EXISTS ent_subscriptions (
    id         UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID        NOT NULL,
    plan_id    UUID        NOT NULL,
    status     VARCHAR(20) NOT NULL,
    started_at TIMESTAMP   NOT NULL,
    expires_at TIMESTAMP,
    created_at TIMESTAMP   NOT NULL,

    CONSTRAINT fk_ent_sub_plan FOREIGN KEY (plan_id) REFERENCES ent_plans(id),
    CONSTRAINT chk_ent_sub_status CHECK (status IN ('ACTIVE', 'CANCELLED', 'PAST_DUE', 'TRIAL'))
);

CREATE INDEX IF NOT EXISTS idx_ent_subscriptions_user_status
    ON ent_subscriptions (user_id, status);
