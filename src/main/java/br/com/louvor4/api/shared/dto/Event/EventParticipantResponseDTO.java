package br.com.louvor4.api.shared.dto.Event;

import br.com.louvor4.api.enums.EventPermission;

import java.util.Set;
import java.util.UUID;

public record EventParticipantResponseDTO(
        UUID memberId,
        String firstName,
        String lastName,
        String profileImage,
        UUID skillId,
        Set<EventPermission>permissions
) {
}
