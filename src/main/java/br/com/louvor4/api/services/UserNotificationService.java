package br.com.louvor4.api.services;

import br.com.louvor4.api.shared.dto.notification.CreateEventInviteNotificationRequest;
import br.com.louvor4.api.shared.dto.notification.CreateUserNotificationRequest;
import br.com.louvor4.api.shared.dto.notification.UnreadNotificationCountResponse;
import br.com.louvor4.api.shared.dto.notification.UserNotificationItemResponse;
import br.com.louvor4.api.shared.dto.notification.UserNotificationListResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface UserNotificationService {
    UserNotificationItemResponse createNotification(CreateUserNotificationRequest request);

    UserNotificationItemResponse createEventInviteNotification(CreateEventInviteNotificationRequest request);

    UserNotificationListResponse listUserNotifications(UUID userId, Pageable pageable);

    UserNotificationListResponse listUnreadNotifications(UUID userId, Pageable pageable);

    UnreadNotificationCountResponse countUnreadNotifications(UUID userId);

    UserNotificationItemResponse markAsRead(UUID userId, UUID notificationId);

    UserNotificationItemResponse markInviteAsReadByEventParticipantId(UUID userId, UUID eventParticipantId);

    void markInviteAsReadByEventParticipantIdIfExists(UUID userId, UUID eventParticipantId);

    long markAllAsRead(UUID userId);
}
