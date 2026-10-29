package br.com.louvor4.api.services;

import com.google.firebase.messaging.FirebaseMessagingException;

import java.util.Map;
import java.util.UUID;

public interface PushSenderService {
    void sendToUser(UUID userId, String title, String message) throws FirebaseMessagingException;

    void sendToUser(UUID userId, String title, String message, String imageUrl) throws FirebaseMessagingException;

    /**
     * @param data pares chave/valor enviados no campo "data" do FCM (ex.: type, eventId),
     *             usados pelo app para abrir a tela certa ao tocar na notificação.
     */
    void sendToUser(UUID userId, String title, String message, String imageUrl, Map<String, String> data)
            throws FirebaseMessagingException;
}
