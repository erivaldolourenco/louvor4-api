package br.com.louvor4.api.shared.dto.Song;

import java.util.UUID;

public record EventSongDTO(UUID id, String title, String artist, String key, Integer bpm, String youTubeUrl, String addedBy) {
}
