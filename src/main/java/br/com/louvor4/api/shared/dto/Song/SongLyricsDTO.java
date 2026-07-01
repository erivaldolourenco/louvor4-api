package br.com.louvor4.api.shared.dto.Song;

import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SongLyricsDTO(
        UUID songId,
        @Size(max = 5000, message = "Letra não pode ultrapassar 5000 caracteres.")
        String lyrics
) {}
