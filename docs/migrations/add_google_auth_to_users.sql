-- Adiciona suporte a login via Google na tabela users

ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(20);
UPDATE users SET auth_provider = 'LOCAL' WHERE auth_provider IS NULL;
ALTER TABLE users ALTER COLUMN auth_provider SET DEFAULT 'LOCAL';
ALTER TABLE users ALTER COLUMN auth_provider SET NOT NULL;

ALTER TABLE users ADD COLUMN IF NOT EXISTS google_id VARCHAR(255);
ALTER TABLE users ADD CONSTRAINT users_google_id_key UNIQUE (google_id);

ALTER TABLE users ALTER COLUMN password DROP NOT NULL;
