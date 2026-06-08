-- Migration: renomeia song_audio_files para audio_files
-- e adiciona suporte a medley_id
-- Executar manualmente antes do deploy

-- 1. Renomear tabela existente
ALTER TABLE song_audio_files RENAME TO audio_files;

-- 2. Renomear constraint e índice existentes
ALTER TABLE audio_files RENAME CONSTRAINT uq_song_audio_song_type TO uq_audio_files_song_type;
ALTER INDEX idx_song_audio_song_id RENAME TO idx_audio_files_song_id;

-- 3. Tornar song_id nullable (agora medley pode ser o owner)
ALTER TABLE audio_files ALTER COLUMN song_id DROP NOT NULL;

-- 4. Adicionar coluna medley_id
ALTER TABLE audio_files ADD COLUMN medley_id UUID REFERENCES medleys(id);

-- 5. CHECK: exatamente um dos dois FKs deve ser preenchido
ALTER TABLE audio_files ADD CONSTRAINT chk_audio_files_owner
    CHECK (
        (song_id IS NOT NULL AND medley_id IS NULL) OR
        (song_id IS NULL AND medley_id IS NOT NULL)
    );

-- 6. Substituir unique constraint de song por índice parcial
ALTER TABLE audio_files DROP CONSTRAINT uq_audio_files_song_type;
CREATE UNIQUE INDEX uq_audio_files_song_type
    ON audio_files(song_id, type)
    WHERE song_id IS NOT NULL;

-- 7. Índice parcial para medley
CREATE UNIQUE INDEX uq_audio_files_medley_type
    ON audio_files(medley_id, type)
    WHERE medley_id IS NOT NULL;

CREATE INDEX idx_audio_files_medley_id
    ON audio_files(medley_id)
    WHERE medley_id IS NOT NULL;

-- ============================================================
-- DDL completo para criação do zero (ambientes limpos)
-- ============================================================
-- CREATE TABLE IF NOT EXISTS audio_files (
--     id         UUID PRIMARY KEY,
--     song_id    UUID REFERENCES songs(id),
--     medley_id  UUID REFERENCES medleys(id),
--     type       VARCHAR(20) NOT NULL,
--     audio_url  VARCHAR(500) NOT NULL,
--     created_at TIMESTAMP NOT NULL,
--     CONSTRAINT chk_audio_files_owner CHECK (
--         (song_id IS NOT NULL AND medley_id IS NULL) OR
--         (song_id IS NULL AND medley_id IS NOT NULL)
--     )
-- );
-- CREATE UNIQUE INDEX IF NOT EXISTS uq_audio_files_song_type
--     ON audio_files(song_id, type) WHERE song_id IS NOT NULL;
-- CREATE UNIQUE INDEX IF NOT EXISTS uq_audio_files_medley_type
--     ON audio_files(medley_id, type) WHERE medley_id IS NOT NULL;
-- CREATE INDEX IF NOT EXISTS idx_audio_files_song_id
--     ON audio_files(song_id) WHERE song_id IS NOT NULL;
-- CREATE INDEX IF NOT EXISTS idx_audio_files_medley_id
--     ON audio_files(medley_id) WHERE medley_id IS NOT NULL;
