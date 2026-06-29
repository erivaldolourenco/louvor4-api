CREATE TABLE IF NOT EXISTS vch_vouchers (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    code          VARCHAR(50)  NOT NULL,
    plan_id       UUID         NOT NULL REFERENCES ent_plans(id),
    duration_days INT          NOT NULL,
    max_uses      INT,
    expires_at    TIMESTAMP,
    is_active     BOOLEAN      NOT NULL DEFAULT true,
    created_at    TIMESTAMP    NOT NULL DEFAULT now(),

    CONSTRAINT uq_vch_vouchers_code UNIQUE (code)
);
