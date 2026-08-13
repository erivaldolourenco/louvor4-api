package br.com.louvor4.api.shared.dto.Event;

import br.com.louvor4.api.enums.EventPermission;

import java.util.Set;

public record EventPermissionsResponseDTO(
        boolean isProjectAdmin,
        Set<EventPermission> permissions
) {
}
