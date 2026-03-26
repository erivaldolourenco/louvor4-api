package br.com.louvor4.api.shared.dto.notification;

import br.com.louvor4.api.enums.NotificationType;

import java.util.UUID;

public record CreateUserNotificationRequest(
        NotificationType type,
        UUID userId,
        String title,
        String message,
        UUID eventParticipantId,
        String dataJson
) {
}
