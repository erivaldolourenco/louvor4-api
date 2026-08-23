package br.com.louvor4.api.shared.dto.Event;

import java.util.List;

public record PublicSetlistMedleyDTO(
        String title,
        List<PublicSetlistSongDTO> songs
) {
}
