package br.com.louvor4.api.shared.dto.Song;

import br.com.louvor4.api.models.SongCategory;

import java.util.UUID;

public record SongCategoryDTO(
        UUID id,
        String name
) {
    public static SongCategoryDTO fromEntity(SongCategory category) {
        return new SongCategoryDTO(category.getId(), category.getName());
    }
}
