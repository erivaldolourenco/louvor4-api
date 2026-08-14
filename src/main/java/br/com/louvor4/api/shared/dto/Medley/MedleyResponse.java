package br.com.louvor4.api.shared.dto.Medley;

import br.com.louvor4.api.shared.dto.Song.SongCategoryDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record MedleyResponse(
        UUID id,
        String name,
        String description,
        String notes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<MedleyItemResponse> items,
        String referenceAudioUrl,
        Set<SongCategoryDTO> categories
) {
}
