-- Músicas do projeto: o dono é o projeto (não um usuário). Exclusão lógica via
-- deleted_at para que repertórios de eventos que já usam a música continuem
-- carregando.
CREATE TABLE IF NOT EXISTS project_songs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    music_project_id UUID NOT NULL REFERENCES music_projects(id),
    created_by_user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    title VARCHAR(150) NOT NULL,
    artist VARCHAR(150) NOT NULL,
    musical_key VARCHAR(5) NOT NULL,
    bpm INT,
    isrc VARCHAR(32),
    album VARCHAR(150),
    youtube_url VARCHAR(255),
    spotify_url VARCHAR(255),
    deezer_url VARCHAR(255),
    cover_url VARCHAR(255),
    notes VARCHAR(1000),
    chord_sheet_json JSONB,
    deleted_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_project_songs_project_id
    ON project_songs (music_project_id);

-- Repertório do evento passa a aceitar música do projeto (tipo PROJECT_SONG).
ALTER TABLE event_setlist_items
    ADD COLUMN IF NOT EXISTS project_song_id UUID REFERENCES project_songs(id);

ALTER TABLE event_setlist_items
    DROP CONSTRAINT IF EXISTS uq_event_setlist_project_song;

ALTER TABLE event_setlist_items
    ADD CONSTRAINT uq_event_setlist_project_song UNIQUE (event_id, project_song_id);

-- PostgreSQL não permite alterar check constraints diretamente — é necessário
-- dropar e recriar.
ALTER TABLE event_setlist_items
    DROP CONSTRAINT IF EXISTS event_setlist_items_type_check;

ALTER TABLE event_setlist_items
    ADD CONSTRAINT event_setlist_items_type_check
        CHECK (type IN ('SONG', 'MEDLEY', 'PROJECT_SONG'));

-- Permissões por membro do projeto (mesmo padrão de event_participant_permissions).
CREATE TABLE IF NOT EXISTS music_project_member_permissions (
    music_project_member_id UUID NOT NULL REFERENCES music_project_members(id) ON DELETE CASCADE,
    permission VARCHAR(50) NOT NULL CHECK (permission IN ('MANAGE_SONGS')),
    PRIMARY KEY (music_project_member_id, permission)
);
