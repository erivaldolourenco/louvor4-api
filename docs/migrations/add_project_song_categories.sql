-- Categorias das músicas do projeto (equivalente a song_categories, mas o dono
-- é o projeto). Depende de add_project_songs.sql.
CREATE TABLE IF NOT EXISTS project_song_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    music_project_id UUID NOT NULL REFERENCES music_projects(id),
    name VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_project_song_category_name UNIQUE (music_project_id, name)
);

CREATE TABLE IF NOT EXISTS project_song_category_assignments (
    project_song_id UUID NOT NULL REFERENCES project_songs(id),
    project_song_category_id UUID NOT NULL REFERENCES project_song_categories(id),
    PRIMARY KEY (project_song_id, project_song_category_id)
);
