package br.com.louvor4.api.shared.dto.notification;

import br.com.louvor4.api.enums.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserNotificationItemResponse(
        UUID id,
        NotificationType type,
        UUID userId,
        String title,
        String message,
        UUID eventParticipantId,
        String dataJson,
        Boolean isRead,
        LocalDateTime createdAt,
        LocalDateTime readAt
) {
}
