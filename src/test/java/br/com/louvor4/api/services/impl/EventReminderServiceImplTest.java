package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.EventParticipantStatus;
import br.com.louvor4.api.enums.ReminderStatus;
import br.com.louvor4.api.models.*;
import br.com.louvor4.api.repositories.EventParticipantRepository;
import br.com.louvor4.api.repositories.EventReminderRepository;
import br.com.louvor4.api.services.EmailService;
import br.com.louvor4.api.services.PushSenderService;
import br.com.louvor4.api.services.UserNotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventReminderServiceImplTest {

    @Mock EventReminderRepository reminderRepository;
    @Mock EventParticipantRepository participantRepository;
    @Mock PushSenderService pushSenderService;
    @Mock UserNotificationService userNotificationService;
    @Mock EmailService emailService;
    @InjectMocks EventReminderServiceImpl service;

    private EventReminder reminder;
    private Event event;
    private User user;
    private EventParticipant acceptedParticipant;
    private EventParticipant pendingParticipant;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");
        user.setFirstName("João");

        MusicProjectMember member = new MusicProjectMember();
        member.setUser(user);

        event = new Event();
        event.setId(UUID.randomUUID());
        event.setTitle("Culto Domingo");
        event.setStartAt(LocalDateTime.now().plusHours(1));

        acceptedParticipant = new EventParticipant();
        acceptedParticipant.setEvent(event);
        acceptedParticipant.setMember(member);
        acceptedParticipant.setStatus(EventParticipantStatus.ACCEPTED);

        pendingParticipant = new EventParticipant();
        pendingParticipant.setEvent(event);
        pendingParticipant.setMember(member);
        pendingParticipant.setStatus(EventParticipantStatus.PENDING);

        reminder = new EventReminder();
        reminder.setEvent(event);
        reminder.setScheduledFor(LocalDateTime.now().minusMinutes(1));
        reminder.setStatus(ReminderStatus.PENDING);
    }

    @Test
    void processDue_skipsWhenAnotherPodClaimedReminder() {
        when(reminderRepository.findByStatusAndScheduledForLessThanEqual(
                eq(ReminderStatus.PENDING), any()))
                .thenReturn(List.of(reminder));
        when(reminderRepository.tryUpdateStatus(
                any(), eq(ReminderStatus.PENDING),
                eq(ReminderStatus.PROCESSING), any()))
                .thenReturn(0);

        service.processDue();

        verify(participantRepository, never()).findByEventId(any());
    }

    @Test
    void processDue_sendsToAcceptedAndPendingParticipants() throws Exception {
        when(reminderRepository.findByStatusAndScheduledForLessThanEqual(
                eq(ReminderStatus.PENDING), any()))
                .thenReturn(List.of(reminder));
        when(reminderRepository.tryUpdateStatus(any(), any(), any(), any())).thenReturn(1);
        when(participantRepository.findByEventId(event.getId()))
                .thenReturn(List.of(acceptedParticipant, pendingParticipant));

        service.processDue();

        verify(pushSenderService, times(2)).sendToUser(eq(user.getId()), anyString(), anyString());
        verify(userNotificationService, times(2)).createNotification(any());
        verify(reminderRepository).markAsSent(any(), any());
    }

    @Test
    void processDue_marksSentEvenWhenPushThrows() throws Exception {
        // Push is @Async in production and cannot throw synchronously — treated as best-effort
        when(reminderRepository.findByStatusAndScheduledForLessThanEqual(
                eq(ReminderStatus.PENDING), any()))
                .thenReturn(List.of(reminder));
        when(reminderRepository.tryUpdateStatus(any(), any(), any(), any())).thenReturn(1);
        when(participantRepository.findByEventId(event.getId()))
                .thenReturn(List.of(acceptedParticipant));
        doThrow(new RuntimeException("Firebase down"))
                .when(pushSenderService).sendToUser(any(), anyString(), anyString());

        service.processDue();

        verify(reminderRepository).markAsSent(any(), any());
        verify(reminderRepository, never()).markAsFailed(any(), any(), any());
    }

    @Test
    void processDue_marksSentEvenWhenEmailThrows() {
        when(reminderRepository.findByStatusAndScheduledForLessThanEqual(
                eq(ReminderStatus.PENDING), any()))
                .thenReturn(List.of(reminder));
        when(reminderRepository.tryUpdateStatus(any(), any(), any(), any())).thenReturn(1);
        when(participantRepository.findByEventId(event.getId()))
                .thenReturn(List.of(acceptedParticipant));
        doThrow(new RuntimeException("Brevo down"))
                .when(emailService).sendEventReminder(anyString(), anyString(), anyString());

        service.processDue();

        verify(reminderRepository).markAsSent(any(), any());
        verify(reminderRepository, never()).markAsFailed(any(), any(), any());
    }

    @Test
    void processDue_recoversStuckProcessingReminders() {
        when(reminderRepository.findByStatusAndScheduledForLessThanEqual(
                eq(ReminderStatus.PENDING), any()))
                .thenReturn(List.of());
        when(reminderRepository.findByStatusAndUpdatedAtBefore(
                eq(ReminderStatus.PROCESSING), any()))
                .thenReturn(List.of(reminder));

        service.processDue();

        verify(reminderRepository).markAsFailed(any(), anyString(), any());
    }
}
