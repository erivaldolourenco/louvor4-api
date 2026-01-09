package br.com.louvor4.api.shared.dto.Event;

import java.util.UUID;

public record EventParticipantResponseDTO(UUID memberId, UUID skillId) {
}
