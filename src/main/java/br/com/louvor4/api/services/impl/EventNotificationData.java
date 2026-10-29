package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.models.Event;
import br.com.louvor4.api.models.MusicProject;

/**
 * Dados de evento compartilhados entre o push e o aviso (dataJson), usados pelo
 * app para abrir a tela do evento e exibir a imagem do projeto.
 */
final class EventNotificationData {

    private EventNotificationData() {
    }

    static String projectImageUrl(Event event) {
        MusicProject project = event.getMusicProject();
        if (project == null) return null;
        String image = project.getProfileImage();
        return image == null || image.isBlank() ? null : image;
    }

    static String dataJson(Event event) {
        String imageUrl = projectImageUrl(event);
        if (imageUrl == null) {
            return String.format("{\"eventId\":\"%s\"}", event.getId());
        }
        return String.format(
                "{\"eventId\":\"%s\",\"projectImageUrl\":\"%s\"}",
                event.getId(), escapeJson(imageUrl));
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
