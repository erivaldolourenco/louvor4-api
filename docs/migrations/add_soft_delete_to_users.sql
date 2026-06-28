-- Adiciona coluna de soft delete na tabela users
ALTER TABLE users ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;

-- Remove a unique constraint existente no username (criada pelo Hibernate via @Column(unique=true))
-- O nome pode variar dependendo do banco; ajuste se necessário
ALTER TABLE users DROP CONSTRAINT IF EXISTS users_username_key;

-- Cria índice único parcial: username único apenas entre usuários não deletados
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username_active
    ON users (username)
    WHERE deleted_at IS NULL;

-- Cria índice único parcial: email único apenas entre usuários não deletados
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email_active
    ON users (email)
    WHERE deleted_at IS NULL AND email IS NOT NULL;
