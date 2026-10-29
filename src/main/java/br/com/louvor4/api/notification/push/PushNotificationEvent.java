package br.com.louvor4.api.notification.push;

import java.util.Map;
import java.util.UUID;

/**
 * @param imageUrl imagem exibida na notificação (ex.: imagem do projeto); pode ser null.
 * @param data payload enviado no campo "data" do FCM. O app usa para decidir
 *             qual tela abrir ao tocar na notificação (ex.: type + eventId).
 */
public record PushNotificationEvent(
        UUID userId, String title, String message, String imageUrl, Map<String, String> data) {

    public PushNotificationEvent(UUID userId, String title, String message) {
        this(userId, title, message, null, Map.of());
    }
}
