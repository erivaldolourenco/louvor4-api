package br.com.louvor4.api.shared.dto.notification;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateEventInviteNotificationRequest(
        UUID userId,
        UUID eventParticipantId,
        String eventTitle,
        LocalDateTime eventStartAt,
        String location,
        String dataJson
) {
}
