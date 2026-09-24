-- Solicitações públicas de exclusão de conta (compliance com a política de exclusão de conta do Google Play)
CREATE TABLE IF NOT EXISTS account_deletion_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    token VARCHAR(64) NOT NULL,
    expiry_date TIMESTAMP NOT NULL
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_account_deletion_requests_token
    ON account_deletion_requests (token);

CREATE INDEX IF NOT EXISTS idx_account_deletion_requests_user_id
    ON account_deletion_requests (user_id);
