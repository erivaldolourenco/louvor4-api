package br.com.louvor4.api.notification.push;

import java.util.UUID;

public record PushNotificationEvent(UUID userId, String title, String message) {
}
