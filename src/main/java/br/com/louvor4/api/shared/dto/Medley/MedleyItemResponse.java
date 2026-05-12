package br.com.louvor4.api.shared.dto.Medley;

import java.util.UUID;

public record MedleyItemResponse(
        UUID id,
        UUID songId,
        String songTitle,
        String songArtist,
        String youTubeUrl,
        String key,
        String notes,
        Integer sequence
) {
}
