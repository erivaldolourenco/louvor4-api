CREATE TABLE IF NOT EXISTS vch_redemptions (
    id               UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    voucher_id       UUID      NOT NULL REFERENCES vch_vouchers(id),
    user_id          UUID      NOT NULL,
    subscription_id  UUID      NOT NULL,
    previous_plan_id UUID      NOT NULL REFERENCES ent_plans(id),
    redeemed_at      TIMESTAMP NOT NULL DEFAULT now(),
    valid_until      TIMESTAMP NOT NULL,
    reverted         BOOLEAN   NOT NULL DEFAULT false
);
