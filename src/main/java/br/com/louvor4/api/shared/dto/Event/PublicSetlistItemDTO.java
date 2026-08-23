package br.com.louvor4.api.shared.dto.Event;

import br.com.louvor4.api.enums.SetlistItemType;

public record PublicSetlistItemDTO(
        SetlistItemType type,
        PublicSetlistSongDTO song,
        PublicSetlistMedleyDTO medley
) {
}
