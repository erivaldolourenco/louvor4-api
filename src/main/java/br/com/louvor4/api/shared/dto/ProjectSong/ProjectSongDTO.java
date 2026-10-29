package br.com.louvor4.api.shared.dto.ProjectSong;

import br.com.louvor4.api.models.ProjectSong;
import br.com.louvor4.api.shared.dto.Song.SongCategoryDTO;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record ProjectSongDTO(
        UUID id,
        UUID projectId,
        String title,
        String artist,
        String key,
        Integer bpm,
        String isrc,
        String album,
        String youTubeUrl,
        String spotifyUrl,
        String deezerUrl,
        String coverUrl,
        String notes,
        UUID createdByUserId,
        boolean hasChordSheet,
        List<SongCategoryDTO> categories
) {
    public static ProjectSongDTO fromEntity(ProjectSong song) {
        return new ProjectSongDTO(
                song.getId(),
                song.getMusicProject().getId(),
                song.getTitle(),
                song.getArtist(),
                song.getKey(),
                song.getBpm(),
                song.getIsrc(),
                song.getAlbum(),
                song.getYouTubeUrl(),
                song.getSpotifyUrl(),
                song.getDeezerUrl(),
                song.getCoverUrl(),
                song.getNotes(),
                song.getCreatedByUserId(),
                song.getChordSheetJson() != null,
                song.getCategories().stream()
                        .map(c -> new SongCategoryDTO(c.getId(), c.getName()))
                        .sorted(Comparator.comparing(SongCategoryDTO::name, String.CASE_INSENSITIVE_ORDER))
                        .toList()
        );
    }
}
