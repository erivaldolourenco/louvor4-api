package br.com.louvor4.api.shared.dto.Song;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddEventSongDTO(
        @NotNull UUID songId,
        String musicalKey // Opcional
) {}
