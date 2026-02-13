package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.models.NotificationDevice;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.NotificationDeviceRepository;
import br.com.louvor4.api.services.NotificationDeviceService;
import br.com.louvor4.api.shared.dto.notification.RegisterDeviceRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class NotificationDeviceServiceImpl implements NotificationDeviceService {

    private final CurrentUserProvider currentUserProvider;
    private final NotificationDeviceRepository deviceRepository;

    public NotificationDeviceServiceImpl(CurrentUserProvider currentUserProvider,
                                         NotificationDeviceRepository deviceRepository) {
        this.currentUserProvider = currentUserProvider;
        this.deviceRepository = deviceRepository;
    }

    @Override
    public void registerDevice(RegisterDeviceRequest req) {
        User user = currentUserProvider.get();

        NotificationDevice device = deviceRepository
                .findByFcmToken(req.fcmToken())
                .orElseGet(NotificationDevice::new);

        device.setUserId(user.getId());
        device.setFcmToken(req.fcmToken());
        device.setPlatform(req.platform());
        device.setDeviceId(req.deviceId());
        device.setEnabled(true);
        device.setLastSeenAt(LocalDateTime.now());

        deviceRepository.save(device);

    }
}
