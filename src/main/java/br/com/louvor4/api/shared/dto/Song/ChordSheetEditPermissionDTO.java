package br.com.louvor4.api.shared.dto.Song;

import jakarta.validation.constraints.NotNull;

public record ChordSheetEditPermissionDTO(
        @NotNull(message = "editPermission é obrigatório.")
        Boolean editPermission
) {}
