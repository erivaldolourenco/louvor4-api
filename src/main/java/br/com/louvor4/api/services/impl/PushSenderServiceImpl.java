package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.models.NotificationDevice;
import br.com.louvor4.api.repositories.NotificationDeviceRepository;
import br.com.louvor4.api.services.PushSenderService;
import com.google.firebase.messaging.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PushSenderServiceImpl implements PushSenderService {

    private final NotificationDeviceRepository deviceRepository;

    public PushSenderServiceImpl(NotificationDeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Async
    @Override
    public void sendToUser(UUID userId, String title, String message) {
        sendToUser(userId, title, message, null);
    }

    @Async
    @Override
    public void sendToUser(UUID userId, String title, String message, String imageUrl) {
        sendToUser(userId, title, message, imageUrl, Map.of());
    }

    @Async
    @Override
    public void sendToUser(UUID userId, String title, String message, String imageUrl, Map<String, String> data) {
        List<NotificationDevice> devices = deviceRepository.findAllByUserIdAndEnabledTrue(userId);
        if (devices.isEmpty()) return;

        List<String> tokens = devices.stream()
                .map(NotificationDevice::getFcmToken)
                .toList();

        Notification.Builder notificationBuilder = Notification.builder()
                .setTitle(title)
                .setBody(message);
        if (imageUrl != null && !imageUrl.isBlank()) {
            notificationBuilder.setImage(imageUrl);
        }

        MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(notificationBuilder.build())
                .setAndroidConfig(AndroidConfig.builder()
                        .setPriority(AndroidConfig.Priority.HIGH)
                        .build())
                .putData("screen", "home")
                .putData("url", "/")
                .putAllData(data == null ? Map.of() : data);
        if (imageUrl != null && !imageUrl.isBlank()) {
            // iOS: mutable-content aciona a extensão ImageNotification do app,
            // que baixa a imagem de fcm_options.image e anexa à notificação.
            messageBuilder.setApnsConfig(ApnsConfig.builder()
                    .setAps(Aps.builder().setMutableContent(true).build())
                    .setFcmOptions(ApnsFcmOptions.builder().setImage(imageUrl).build())
                    .build());
        }
        MulticastMessage multicastMessage = messageBuilder.build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(multicastMessage);
            handleFailedTokens(response, devices);
        } catch (FirebaseMessagingException e) {
            System.err.println("Erro ao enviar push: " + e.getMessage());
        }
    }


    private void handleFailedTokens(BatchResponse response, List<NotificationDevice> devices) {
        if (response.getFailureCount() > 0) {
            List<SendResponse> responses = response.getResponses();
            for (int i = 0; i < responses.size(); i++) {
                if (!responses.get(i).isSuccessful()) {
                    // Se o erro for que o token é inválido, desabilite no banco
                    NotificationDevice device = devices.get(i);
                    device.setEnabled(false);
                    deviceRepository.save(device);
                }
            }
        }
    }
}
