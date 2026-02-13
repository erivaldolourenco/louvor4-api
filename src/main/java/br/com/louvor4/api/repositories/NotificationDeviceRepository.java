package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.NotificationDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationDeviceRepository extends JpaRepository<NotificationDevice, UUID> {
    Optional<NotificationDevice> findByFcmToken(String fcmToken);
    List<NotificationDevice> findAllByUserIdAndEnabledTrue(UUID userId);
}
