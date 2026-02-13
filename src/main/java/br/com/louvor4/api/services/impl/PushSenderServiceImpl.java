package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.models.NotificationDevice;
import br.com.louvor4.api.repositories.NotificationDeviceRepository;
import br.com.louvor4.api.services.PushSenderService;
import com.google.firebase.messaging.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
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
        List<NotificationDevice> devices = deviceRepository.findAllByUserIdAndEnabledTrue(userId);
        if (devices.isEmpty()) return;

        List<String> tokens = devices.stream()
                .map(NotificationDevice::getFcmToken)
                .toList();

        MulticastMessage multicastMessage = MulticastMessage.builder()
                .addAllTokens(tokens)
                .setNotification(Notification.builder()
                        .setTitle(title)
                        .setBody(message)
                        .build())
                .setWebpushConfig(WebpushConfig.builder()
                        .setNotification(WebpushNotification.builder()
                                .setIcon("https://app.louvor4.com.br/images/logo/logo-icon.svg")
                                .setVibrate(new int[]{200, 100, 200})
                                .build())
                        .build())
                .putData("screen", "events")
                .build();

        try {
            BatchResponse response = FirebaseMessaging.getInstance().sendEachForMulticast(multicastMessage);
            handleFailedTokens(response, devices);
        } catch (FirebaseMessagingException e) {
            // Logar o erro adequadamente
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
