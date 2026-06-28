-- Tabela: ent_plan_entitlements
-- Valor de cada entitlement por plano (ex: max_musicas = 30 no FREE)
-- Valor -1 = ilimitado

CREATE TABLE IF NOT EXISTS ent_plan_entitlements (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_id        UUID        NOT NULL,
    entitlement_id UUID        NOT NULL,
    value          VARCHAR(50) NOT NULL,

    CONSTRAINT fk_ent_pe_plan        FOREIGN KEY (plan_id)        REFERENCES ent_plans(id),
    CONSTRAINT fk_ent_pe_entitlement FOREIGN KEY (entitlement_id) REFERENCES ent_entitlements(id),
    CONSTRAINT uq_ent_plan_entitlement UNIQUE (plan_id, entitlement_id)
);
