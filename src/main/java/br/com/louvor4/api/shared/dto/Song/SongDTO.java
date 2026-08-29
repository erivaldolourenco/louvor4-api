package br.com.louvor4.api.shared.dto.Song;

import java.util.Set;
import java.util.UUID;

public record SongDTO(
        UUID id,
        UUID userId,
        String title,
        String artist,
        String key,
        Integer bpm,
        String isrc,
        String album,
        String youTubeUrl,
        String spotifyUrl,
        String deezerUrl,
        String coverUrl,
        String notes,
        String referenceAudioUrl,
        String vsAudioUrl,
        Set<SongCategoryDTO> categories
) {}
