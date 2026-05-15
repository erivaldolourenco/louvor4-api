package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.NotificationType;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.UserNotification;
import br.com.louvor4.api.repositories.UserNotificationRepository;
import br.com.louvor4.api.services.UserNotificationService;
import br.com.louvor4.api.shared.dto.notification.CreateEventInviteNotificationRequest;
import br.com.louvor4.api.shared.dto.notification.CreateUserNotificationRequest;
import br.com.louvor4.api.shared.dto.notification.UnreadNotificationCountResponse;
import br.com.louvor4.api.shared.dto.notification.UserNotificationItemResponse;
import br.com.louvor4.api.shared.dto.notification.UserNotificationListResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@Service
public class UserNotificationServiceImpl implements UserNotificationService {

    private static final DateTimeFormatter EVENT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter EVENT_TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final UserNotificationRepository userNotificationRepository;

    public UserNotificationServiceImpl(UserNotificationRepository userNotificationRepository) {
        this.userNotificationRepository = userNotificationRepository;
    }

    @Override
    @Transactional
    public UserNotificationItemResponse createNotification(CreateUserNotificationRequest request) {
        validateCreateRequest(request);

        UserNotification notification = new UserNotification();
        notification.setType(request.type());
        notification.setUserId(request.userId());
        notification.setTitle(request.title().trim());
        notification.setMessage(request.message().trim());
        notification.setEventParticipantId(request.eventParticipantId());
        notification.setDataJson(normalizeBlank(request.dataJson()));

        return toResponse(userNotificationRepository.save(notification));
    }

    @Override
    @Transactional
    public UserNotificationItemResponse createEventInviteNotification(CreateEventInviteNotificationRequest request) {
        validateEventInviteRequest(request);

        String title = "Convite para evento: " + request.eventTitle().trim();
        String message = buildEventInviteMessage(request.eventTitle(), request.eventStartAt(), request.location());

        return createNotification(new CreateUserNotificationRequest(
                NotificationType.EVENT_INVITE,
                request.userId(),
                title,
                message,
                request.eventParticipantId(),
                request.dataJson()
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public UserNotificationListResponse listUserNotifications(UUID userId, Pageable pageable) {
        validateUserId(userId);
        Page<UserNotification> page = userNotificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return toListResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public UserNotificationListResponse listUnreadNotifications(UUID userId, Pageable pageable) {
        validateUserId(userId);
        Page<UserNotification> page = userNotificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId, pageable);
        return toListResponse(page);
    }

    @Override
    @Transactional(readOnly = true)
    public UnreadNotificationCountResponse countUnreadNotifications(UUID userId) {
        validateUserId(userId);
        return new UnreadNotificationCountResponse(userId, userNotificationRepository.countByUserIdAndIsReadFalse(userId));
    }

    @Override
    @Transactional
    public UserNotificationItemResponse markAsRead(UUID userId, UUID notificationId) {
        validateUserId(userId);
        if (notificationId == null) {
            throw new ValidationException("Id da notificação é obrigatório.");
        }

        UserNotification notification = userNotificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new NotFoundException("Notificação não encontrada."));

        notification.markAsRead();
        return toResponse(userNotificationRepository.save(notification));
    }

    @Override
    @Transactional
    public UserNotificationItemResponse markInviteAsReadByEventParticipantId(UUID userId, UUID eventParticipantId) {
        validateUserId(userId);
        if (eventParticipantId == null) {
            throw new ValidationException("Id do participante do evento da notificação é obrigatório.");
        }

        UserNotification notification = userNotificationRepository
                .findFirstByUserIdAndTypeAndEventParticipantIdOrderByCreatedAtDesc(
                        userId,
                        NotificationType.EVENT_INVITE,
                        eventParticipantId
                )
                .orElseThrow(() -> new NotFoundException("Notificação de convite não encontrada."));

        notification.markAsRead();
        return toResponse(userNotificationRepository.save(notification));
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markInviteAsReadByEventParticipantIdIfExists(UUID userId, UUID eventParticipantId) {
        if (userId == null || eventParticipantId == null) {
            return;
        }

        userNotificationRepository
                .findFirstByUserIdAndTypeAndEventParticipantIdOrderByCreatedAtDesc(
                        userId,
                        NotificationType.EVENT_INVITE,
                        eventParticipantId
                )
                .ifPresent(notification -> {
                    notification.markAsRead();
                    userNotificationRepository.save(notification);
                });
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markProjectInviteAsReadIfExists(UUID userId, UUID projectId) {
        if (userId == null || projectId == null) {
            return;
        }

        userNotificationRepository
                .findFirstProjectInviteByUserIdAndProjectId(userId, projectId)
                .ifPresent(notification -> {
                    notification.markAsRead();
                    userNotificationRepository.save(notification);
                });
    }

    @Override
    @Transactional
    public long markAllAsRead(UUID userId) {
        validateUserId(userId);
        return userNotificationRepository.markAllAsReadByUserId(userId);
    }

    private void validateCreateRequest(CreateUserNotificationRequest request) {
        if (request == null) {
            throw new ValidationException("Dados da notificação são obrigatórios.");
        }
        if (request.type() == null) {
            throw new ValidationException("Tipo da notificação é obrigatório.");
        }
        validateUserId(request.userId());
        if (isBlank(request.title())) {
            throw new ValidationException("Título da notificação é obrigatório.");
        }
        if (isBlank(request.message())) {
            throw new ValidationException("Mensagem da notificação é obrigatória.");
        }
    }

    private void validateEventInviteRequest(CreateEventInviteNotificationRequest request) {
        if (request == null) {
            throw new ValidationException("Dados do convite são obrigatórios.");
        }
        validateUserId(request.userId());
        if (request.eventParticipantId() == null) {
            throw new ValidationException("Id do participante do evento é obrigatório.");
        }
        if (isBlank(request.eventTitle())) {
            throw new ValidationException("Título do evento é obrigatório.");
        }
    }

    private void validateUserId(UUID userId) {
        if (userId == null) {
            throw new ValidationException("Id do usuário é obrigatório.");
        }
    }

    private UserNotificationListResponse toListResponse(Page<UserNotification> page) {
        List<UserNotificationItemResponse> items = page.getContent()
                .stream()
                .map(this::toResponse)
                .toList();

        return new UserNotificationListResponse(
                items,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    private UserNotificationItemResponse toResponse(UserNotification notification) {
        return new UserNotificationItemResponse(
                notification.getId(),
                notification.getType(),
                notification.getUserId(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getEventParticipantId(),
                notification.getDataJson(),
                notification.getIsRead(),
                notification.getCreatedAt(),
                notification.getReadAt()
        );
    }

    private String buildEventInviteMessage(String eventTitle, LocalDateTime eventStartAt, String location) {
        StringBuilder builder = new StringBuilder("Voce recebeu um convite para o evento ");
        builder.append(eventTitle.trim()).append(".");

        if (eventStartAt != null) {
            builder.append(" Data: ")
                    .append(eventStartAt.format(EVENT_DATE_FORMATTER))
                    .append(" as ")
                    .append(eventStartAt.format(EVENT_TIME_FORMATTER))
                    .append(".");
        }

        if (!isBlank(location)) {
            builder.append(" Local: ").append(location.trim()).append(".");
        }

        return builder.toString();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeBlank(String value) {
        return isBlank(value) ? null : value.trim();
    }
}
