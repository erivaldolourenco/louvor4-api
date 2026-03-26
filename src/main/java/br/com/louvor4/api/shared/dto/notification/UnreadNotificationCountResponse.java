package br.com.louvor4.api.shared.dto.notification;

import java.util.UUID;

public record UnreadNotificationCountResponse(
        UUID userId,
        long unreadCount
) {
}
