package br.com.louvor4.api.repositories;

import br.com.louvor4.api.enums.NotificationType;
import br.com.louvor4.api.models.UserNotification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserNotificationRepository extends JpaRepository<UserNotification, UUID> {
    Page<UserNotification> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    Page<UserNotification> findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    long countByUserIdAndIsReadFalse(UUID userId);

    Optional<UserNotification> findByIdAndUserId(UUID id, UUID userId);

    Optional<UserNotification> findFirstByUserIdAndTypeAndEventParticipantIdOrderByCreatedAtDesc(
            UUID userId,
            NotificationType type,
            UUID eventParticipantId
    );

    Page<UserNotification> findByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, NotificationType type, Pageable pageable);

    @Query("""
            SELECT n FROM UserNotification n
             WHERE n.userId = :userId
               AND n.type = 'PROJECT_MEMBER_INVITE'
               AND n.dataJson LIKE CONCAT('%"projectId":"', CAST(:projectId AS string), '"%')
             ORDER BY n.createdAt DESC
            """)
    Optional<UserNotification> findFirstProjectInviteByUserIdAndProjectId(
            @Param("userId") UUID userId,
            @Param("projectId") UUID projectId
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update UserNotification notification
               set notification.isRead = true,
                   notification.readAt = CURRENT_TIMESTAMP
             where notification.userId = :userId
               and notification.isRead = false
            """)
    int markAllAsReadByUserId(@Param("userId") UUID userId);
}
