-- Seed: planos, entitlements e valores por plano
-- Executar após criar todas as tabelas (01 a 06)

-- Planos
INSERT INTO ent_plans (id, name, display_name, price) VALUES
    (gen_random_uuid(), 'FREE',    'Gratuito', 0.00),
    (gen_random_uuid(), 'STARTER', 'Starter',  9.90),
    (gen_random_uuid(), 'PRO',     'Pro',       19.90),
    (gen_random_uuid(), 'ELITE',   'Elite',     29.90),
    (gen_random_uuid(), 'PARTNER', 'Parceiro',  0.00)
ON CONFLICT (name) DO NOTHING;

-- Catálogo de entitlements
INSERT INTO ent_entitlements (id, key, type, description) VALUES
    (gen_random_uuid(), 'max_songs',            'LIMIT', 'Número máximo de músicas cadastradas'),
    (gen_random_uuid(), 'upload_audio',         'FLAG',  'Envio de arquivos de áudio (mp3, wav, etc)'),
    (gen_random_uuid(), 'max_projects',         'LIMIT', 'Número máximo de projetos'),
    (gen_random_uuid(), 'max_project_members',  'LIMIT', 'Número máximo de membros em um projeto')
ON CONFLICT (key) DO NOTHING;

-- Valores por plano
INSERT INTO ent_plan_entitlements (id, plan_id, entitlement_id, value)
SELECT gen_random_uuid(), p.id, e.id, v.value
FROM (VALUES
    ('FREE',    'max_songs',           '7'),
    ('FREE',    'upload_audio',        'false'),
    ('FREE',    'max_projects',        '1'),
    ('FREE',    'max_project_members', '10'),

    ('STARTER', 'max_songs',           '15'),
    ('STARTER', 'upload_audio',        'true'),
    ('STARTER', 'max_projects',        '3'),
    ('STARTER', 'max_project_members', '10'),

    ('PRO',     'max_songs',           '20'),
    ('PRO',     'upload_audio',        'true'),
    ('PRO',     'max_projects',        '5'),
    ('PRO',     'max_project_members', '20'),

    ('ELITE',   'max_songs',           '40'),
    ('ELITE',   'upload_audio',        'true'),
    ('ELITE',   'max_projects',        '10'),
    ('ELITE',   'max_project_members', '30'),

    ('PARTNER', 'max_songs',           '50'),
    ('PARTNER', 'upload_audio',        'true'),
    ('PARTNER', 'max_projects',        '10'),
    ('PARTNER', 'max_project_members', '20')
) AS v(plan_name, entitlement_key, value)
JOIN ent_plans        p ON p.name = v.plan_name
JOIN ent_entitlements e ON e.key  = v.entitlement_key
ON CONFLICT (plan_id, entitlement_id) DO NOTHING;
