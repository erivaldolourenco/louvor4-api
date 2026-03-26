package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.models.UserNotification;
import br.com.louvor4.api.repositories.UserNotificationRepository;
import br.com.louvor4.api.shared.dto.notification.UserNotificationListResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserNotificationServiceImplTest {

    @Mock
    private UserNotificationRepository userNotificationRepository;

    @InjectMocks
    private UserNotificationServiceImpl userNotificationService;

    @Test
    void listUnreadNotificationsShouldReturnOnlyUnreadItemsFromRepository() {
        UUID userId = UUID.randomUUID();
        Pageable pageable = PageRequest.of(0, 10);

        UserNotification unreadNotification = new UserNotification();
        unreadNotification.setId(UUID.randomUUID());
        unreadNotification.setUserId(userId);
        unreadNotification.setTitle("Titulo");
        unreadNotification.setMessage("Mensagem");
        unreadNotification.setEventParticipantId(UUID.randomUUID());
        unreadNotification.setIsRead(false);
        unreadNotification.setCreatedAt(LocalDateTime.now());

        when(userNotificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable))
                .thenReturn(new PageImpl<>(List.of(unreadNotification), pageable, 1));

        UserNotificationListResponse response = userNotificationService.listUnreadNotifications(userId, pageable);

        verify(userNotificationRepository).findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
        assertEquals(1, response.items().size());
        assertFalse(response.items().get(0).isRead());
    }
}
