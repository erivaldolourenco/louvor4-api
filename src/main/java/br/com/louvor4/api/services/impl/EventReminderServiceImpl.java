package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.EventParticipantStatus;
import br.com.louvor4.api.enums.NotificationType;
import br.com.louvor4.api.enums.ReminderStatus;
import br.com.louvor4.api.models.EventParticipant;
import br.com.louvor4.api.models.EventReminder;
import br.com.louvor4.api.repositories.EventParticipantRepository;
import br.com.louvor4.api.repositories.EventReminderRepository;
import br.com.louvor4.api.services.EmailService;
import br.com.louvor4.api.services.EventReminderService;
import br.com.louvor4.api.services.PushSenderService;
import br.com.louvor4.api.services.UserNotificationService;
import br.com.louvor4.api.shared.dto.notification.CreateUserNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EventReminderServiceImpl implements EventReminderService {

    private static final Logger log = LoggerFactory.getLogger(EventReminderServiceImpl.class);
    private static final int STUCK_PROCESSING_MINUTES = 10;

    private final EventReminderRepository reminderRepository;
    private final EventParticipantRepository participantRepository;
    private final PushSenderService pushSenderService;
    private final UserNotificationService userNotificationService;
    private final EmailService emailService;

    public EventReminderServiceImpl(
            EventReminderRepository reminderRepository,
            EventParticipantRepository participantRepository,
            PushSenderService pushSenderService,
            UserNotificationService userNotificationService,
            EmailService emailService) {
        this.reminderRepository = reminderRepository;
        this.participantRepository = participantRepository;
        this.pushSenderService = pushSenderService;
        this.userNotificationService = userNotificationService;
        this.emailService = emailService;
    }

    @Override
    public void processDue() {
        recoverStuck();

        LocalDateTime now = LocalDateTime.now();
        List<EventReminder> due = reminderRepository
                .findByStatusAndScheduledForLessThanEqual(ReminderStatus.PENDING, now);

        for (EventReminder reminder : due) {
            int claimed = reminderRepository.tryUpdateStatus(
                    reminder.getId(), ReminderStatus.PENDING, ReminderStatus.PROCESSING, now);
            if (claimed == 0) continue;

            try {
                sendForReminder(reminder);
                reminderRepository.markAsSent(reminder.getId(), LocalDateTime.now());
            } catch (Exception e) {
                log.error("Falha ao processar lembrete {}: {}", reminder.getId(), e.getMessage(), e);
                reminderRepository.markAsFailed(reminder.getId(), e.getMessage(), LocalDateTime.now());
            }
        }
    }

    private void recoverStuck() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(STUCK_PROCESSING_MINUTES);
        List<EventReminder> stuck = reminderRepository
                .findByStatusAndUpdatedAtBefore(ReminderStatus.PROCESSING, threshold);
        LocalDateTime now = LocalDateTime.now();
        stuck.forEach(r -> reminderRepository.markAsFailed(r.getId(), "Stuck in PROCESSING", now));
    }

    private void sendForReminder(EventReminder reminder) {
        List<EventParticipant> participants =
                participantRepository.findByEventId(reminder.getEvent().getId());

        for (EventParticipant p : participants) {
            if (p.getStatus() == EventParticipantStatus.ACCEPTED) {
                sendReminderToAccepted(reminder, p);
            } else if (p.getStatus() == EventParticipantStatus.PENDING) {
                sendNudgeToPending(reminder, p);
            }
        }
    }

    private void sendReminderToAccepted(EventReminder reminder, EventParticipant participant) {
        UUID userId = participant.getMember().getUser().getId();
        String title = "Lembrete: " + reminder.getEvent().getTitle();
        String message = reminder.getEvent().getTitle() + " acontece amanhã. Até lá!";
        sendAll(userId, participant.getMember().getUser().getEmail(), title, message);
    }

    private void sendNudgeToPending(EventReminder reminder, EventParticipant participant) {
        UUID userId = participant.getMember().getUser().getId();
        String title = reminder.getEvent().getTitle() + " é amanhã";
        String message = "Você ainda não confirmou sua presença em "
                + reminder.getEvent().getTitle() + ". Acesse o app para responder.";
        sendAll(userId, participant.getMember().getUser().getEmail(), title, message);
    }

    private void sendAll(UUID userId, String email, String title, String message) {
        // Push is @Async — exceptions cannot surface synchronously; treat as best-effort
        try {
            pushSenderService.sendToUser(userId, title, message);
        } catch (Exception e) {
            log.warn("Push falhou para usuário {}: {}", userId, e.getMessage());
        }

        try {
            userNotificationService.createNotification(new CreateUserNotificationRequest(
                    NotificationType.EVENT_REMINDER,
                    userId,
                    title,
                    message,
                    null,
                    null
            ));
        } catch (Exception e) {
            log.warn("Notificação interna falhou para usuário {}: {}", userId, e.getMessage());
        }

        try {
            emailService.sendEventReminder(email, title, message);
        } catch (Exception e) {
            log.warn("E-mail falhou para {}: {}", email, e.getMessage());
        }
    }
}
