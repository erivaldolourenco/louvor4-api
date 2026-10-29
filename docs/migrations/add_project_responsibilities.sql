-- Responsabilidades do projeto (Mural): funções fixas que ficam com um membro
-- por período, ex.: "Direção musical". Os períodos/responsáveis ficam em uma
-- tabela de histórico separada (próxima migração).
CREATE TABLE IF NOT EXISTS project_responsibilities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    music_project_id UUID NOT NULL REFERENCES music_projects(id),
    name VARCHAR(80) NOT NULL,
    description VARCHAR(500),
    position INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_project_responsibilities_project_id
    ON project_responsibilities (music_project_id);
