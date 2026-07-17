package br.com.louvor4.api.shared.dto.Song;

import java.util.UUID;

public record EventSongDTO(
        UUID id,
        UUID songId,
        String title,
        String artist,
        String key,
        Integer bpm,
        String youTubeUrl,
        String notes,
        String addedBy,
        String referenceAudioUrl,
        Boolean editChordSheetPermission
) {
}
