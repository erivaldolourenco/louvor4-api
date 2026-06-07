-- DDL para produção: executar manualmente antes do deploy
CREATE TABLE IF NOT EXISTS song_audio_files (
    id         UUID PRIMARY KEY,
    song_id    UUID NOT NULL REFERENCES songs(id),
    type       VARCHAR(20) NOT NULL,
    audio_url  VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uq_song_audio_song_type UNIQUE (song_id, type)
);

CREATE INDEX IF NOT EXISTS idx_song_audio_song_id ON song_audio_files(song_id);
