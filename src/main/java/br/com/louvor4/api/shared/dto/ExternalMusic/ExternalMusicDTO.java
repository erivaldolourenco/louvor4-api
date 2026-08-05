package br.com.louvor4.api.shared.dto.ExternalMusic;

import br.com.louvor4.api.enums.MusicProvider;

public record ExternalMusicDTO(
        String externalId,
        String title,
        String artist,
        String album,
        String coverUrl,
        Long durationMs,
        String previewUrl,
        String spotifyUrl,
        String deezerUrl,
        String isrc,
        Integer bpm,
        MusicProvider provider
) {}
