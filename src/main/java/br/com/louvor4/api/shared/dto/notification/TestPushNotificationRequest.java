package br.com.louvor4.api.shared.dto.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record TestPushNotificationRequest(
        @NotNull UUID userId,
        @NotBlank String title,
        @NotBlank String message,
        String imageUrl
) {
}
