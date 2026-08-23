package br.com.louvor4.api.shared.dto.Event;

public record PublicSetlistSongDTO(
        String artist,
        String title,
        String key,
        Integer bpm,
        String youTubeUrl,
        String spotifyUrl,
        String deezerUrl
) {
}
