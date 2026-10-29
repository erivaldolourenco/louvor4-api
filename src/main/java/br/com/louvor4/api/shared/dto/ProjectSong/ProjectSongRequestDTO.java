package br.com.louvor4.api.shared.dto.ProjectSong;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ProjectSongRequestDTO(
        @NotBlank(message = "Informe o título da música.")
        @Size(min = 3, max = 150, message = "O título deve ter entre 3 e 150 caracteres.")
        String title,
        @NotBlank(message = "Informe o artista.")
        @Size(min = 3, max = 150, message = "O artista deve ter entre 3 e 150 caracteres.")
        String artist,
        @NotBlank(message = "Informe o tom.")
        @Pattern(regexp = "^[A-G](#|b)?m?$", message = "Tom inválido. Use C, D, E, F, G, A ou B com #, b e/ou m.")
        String key,
        Integer bpm,
        @Size(max = 32) String isrc,
        @Size(max = 150) String album,
        @Size(max = 255) String youTubeUrl,
        @Size(max = 255) String spotifyUrl,
        @Size(max = 255) String deezerUrl,
        @Size(max = 255) String coverUrl,
        @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres.") String notes
) {
}
