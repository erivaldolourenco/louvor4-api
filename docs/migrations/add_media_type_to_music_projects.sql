-- Adiciona 'MEDIA' aos valores aceitos pela check constraint de type em
-- music_projects (novo MusicProjectType.MEDIA para equipes de mídia sem
-- repertório). PostgreSQL não permite alterar check constraints diretamente
-- — é necessário dropar e recriar.
ALTER TABLE music_projects
    DROP CONSTRAINT music_projects_type_check;

ALTER TABLE music_projects
    ADD CONSTRAINT music_projects_type_check
        CHECK (type IN ('MINISTRY', 'BAND', 'SINGER', 'MEDIA'));
