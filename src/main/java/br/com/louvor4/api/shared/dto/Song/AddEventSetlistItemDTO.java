package br.com.louvor4.api.shared.dto.Song;

import br.com.louvor4.api.enums.SetlistItemType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddEventSetlistItemDTO(
        @NotNull UUID itemId,
        @NotNull SetlistItemType type
) {}
