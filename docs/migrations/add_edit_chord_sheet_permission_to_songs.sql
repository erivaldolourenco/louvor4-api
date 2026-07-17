-- Adiciona permissão de edição de cifra na tabela songs

ALTER TABLE songs ADD COLUMN IF NOT EXISTS edit_chord_sheet_permission BOOLEAN;
UPDATE songs SET edit_chord_sheet_permission = false WHERE edit_chord_sheet_permission IS NULL;
ALTER TABLE songs ALTER COLUMN edit_chord_sheet_permission SET DEFAULT false;
ALTER TABLE songs ALTER COLUMN edit_chord_sheet_permission SET NOT NULL;
