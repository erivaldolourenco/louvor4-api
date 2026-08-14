package br.com.louvor4.api.shared.dto.Song;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SongCategoryRequestDTO(
        @NotBlank @Size(max = 50) String name
) {
}
