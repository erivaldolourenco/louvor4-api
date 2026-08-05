-- Adiciona spotify_url, deezer_url e cover_url e torna youtube_url opcional na tabela songs

ALTER TABLE songs ADD COLUMN IF NOT EXISTS spotify_url VARCHAR(255);
ALTER TABLE songs ADD COLUMN IF NOT EXISTS deezer_url VARCHAR(255);
ALTER TABLE songs ADD COLUMN IF NOT EXISTS cover_url VARCHAR(255);
ALTER TABLE songs ALTER COLUMN youtube_url DROP NOT NULL;
