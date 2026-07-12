-- Adiciona suporte a chordSheet (cifra) na tabela songs

ALTER TABLE songs ADD COLUMN IF NOT EXISTS chord_sheet_json JSONB NULL;
