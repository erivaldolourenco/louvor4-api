-- Adiciona soft delete em events e event_participants para preservar histórico
-- quando um projeto é deletado (membros devem continuar vendo os eventos que participaram)

ALTER TABLE events ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;
ALTER TABLE event_participants ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMP NULL;
