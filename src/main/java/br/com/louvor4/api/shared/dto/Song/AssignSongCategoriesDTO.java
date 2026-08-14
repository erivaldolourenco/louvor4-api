package br.com.louvor4.api.shared.dto.Song;

import java.util.Set;
import java.util.UUID;

public record AssignSongCategoriesDTO(
        Set<UUID> categoryIds
) {
}
