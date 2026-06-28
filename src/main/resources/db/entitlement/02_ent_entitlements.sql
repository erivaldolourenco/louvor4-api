-- Tabela: ent_entitlements
-- Catálogo de funcionalidades controláveis (FLAGS, LIMITs e QUOTAs)

CREATE TABLE IF NOT EXISTS ent_entitlements (
    id          UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    key         VARCHAR(100) NOT NULL,
    type        VARCHAR(10)  NOT NULL,
    description VARCHAR(255),

    CONSTRAINT uq_ent_entitlements_key UNIQUE (key),
    CONSTRAINT chk_ent_entitlements_type CHECK (type IN ('FLAG', 'LIMIT', 'QUOTA'))
);
