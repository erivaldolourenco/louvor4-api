package br.com.louvor4.api.shared.dto.Event;

import br.com.louvor4.api.enums.SetlistItemType;
import br.com.louvor4.api.shared.dto.Song.EventSongDTO;

import java.util.UUID;

public record SetlistDTO(
        UUID id,
        SetlistItemType type,
        String addedBy,
        String notes,
        EventSongDTO eventSong,
        EventMedleyDTO eventMedley
) {
}
