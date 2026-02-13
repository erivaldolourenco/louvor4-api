package br.com.louvor4.api.services;

import com.google.firebase.messaging.FirebaseMessagingException;

import java.util.UUID;

public interface PushSenderService {
    void sendToUser(UUID userId, String title, String message) throws FirebaseMessagingException;
}
