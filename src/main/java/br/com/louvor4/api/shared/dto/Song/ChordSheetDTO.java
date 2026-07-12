package br.com.louvor4.api.shared.dto.Song;

import java.util.UUID;

public record ChordSheetDTO(
        UUID songId,
        String chordSheetJson
) {}
