package br.com.louvor4.api.models;

import br.com.louvor4.api.enums.NotificationType;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "user_notification",
        indexes = {
                @Index(name = "idx_user_notification_user_created_at", columnList = "user_id, created_at"),
                @Index(name = "idx_user_notification_user_is_read_created_at", columnList = "user_id, is_read, created_at"),
                @Index(name = "idx_user_notification_user_type_created_at", columnList = "user_id, type, created_at"),
                @Index(name = "idx_user_notification_event_participant", columnList = "event_participant_id")
        }
)
public class UserNotification {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 60)
    private NotificationType type;

    @Column(name = "user_id", nullable = false, columnDefinition = "uuid")
    private UUID userId;

    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "message", nullable = false, length = 500)
    private String message;

    @Column(name = "event_participant_id", columnDefinition = "uuid")
    private UUID eventParticipantId;

    @Column(name = "data_json", columnDefinition = "TEXT")
    private String dataJson;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.isRead == null) {
            this.isRead = Boolean.FALSE;
        }
    }

    public void markAsRead() {
        if (Boolean.TRUE.equals(this.isRead)) {
            if (this.readAt == null) {
                this.readAt = LocalDateTime.now();
            }
            return;
        }

        this.isRead = Boolean.TRUE;
        this.readAt = LocalDateTime.now();
    }

    public void markAsUnread() {
        this.isRead = Boolean.FALSE;
        this.readAt = null;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDataJson() {
        return dataJson;
    }

    public void setDataJson(String dataJson) {
        this.dataJson = dataJson;
    }

    public UUID getEventParticipantId() {
        return eventParticipantId;
    }

    public void setEventParticipantId(UUID eventParticipantId) {
        this.eventParticipantId = eventParticipantId;
    }

    public Boolean getIsRead() {
        return isRead;
    }

    public void setIsRead(Boolean isRead) {
        this.isRead = isRead;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}
