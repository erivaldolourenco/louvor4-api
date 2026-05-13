package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.ReminderStatus;
import br.com.louvor4.api.models.Event;
import br.com.louvor4.api.models.EventReminder;
import br.com.louvor4.api.repositories.EventReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventReminderSchedulerImplTest {

    @Mock EventReminderRepository repository;
    @InjectMocks EventReminderSchedulerImpl scheduler;

    private Event event;

    @BeforeEach
    void setUp() {
        event = new Event();
        event.setId(UUID.randomUUID());
    }

    @Test
    void schedule_createsReminderWhen25hBefore() {
        event.setStartAt(LocalDateTime.now().plusHours(25));

        scheduler.schedule(event);

        ArgumentCaptor<EventReminder> captor = ArgumentCaptor.forClass(EventReminder.class);
        verify(repository).save(captor.capture());
        EventReminder saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(saved.getScheduledFor())
                .isCloseTo(event.getStartAt().minusHours(24), within(5, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    void schedule_doesNotCreateReminderWhen23hBefore() {
        event.setStartAt(LocalDateTime.now().plusHours(23));

        scheduler.schedule(event);

        verify(repository, never()).save(any());
    }

    @Test
    void schedule_doesNotCreateReminderWhenExactly24h() {
        event.setStartAt(LocalDateTime.now().plusHours(24));

        scheduler.schedule(event);

        verify(repository, never()).save(any());
    }

    @Test
    void cancel_cancelsExistingPendingReminder() throws Exception {
        UUID eventId = event.getId();
        EventReminder reminder = new EventReminder();
        setId(reminder, UUID.randomUUID());
        reminder.setStatus(ReminderStatus.PENDING);
        when(repository.findByEventIdAndStatusIn(eq(eventId), anyList()))
                .thenReturn(List.of(reminder));

        scheduler.cancel(eventId);

        verify(repository).cancelById(eq(reminder.getId()), any(LocalDateTime.class));
    }

    @Test
    void cancel_doesNothingWhenNoReminderExists() {
        UUID eventId = event.getId();
        when(repository.findByEventIdAndStatusIn(eq(eventId), anyList()))
                .thenReturn(List.of());

        scheduler.cancel(eventId);

        verify(repository, never()).cancelById(any(), any());
    }

    @Test
    void reschedule_cancelsPreviousAndCreatesNew() throws Exception {
        event.setStartAt(LocalDateTime.now().plusHours(30));
        EventReminder existing = new EventReminder();
        setId(existing, UUID.randomUUID());
        existing.setStatus(ReminderStatus.PENDING);
        when(repository.findByEventIdAndStatusIn(eq(event.getId()), anyList()))
                .thenReturn(List.of(existing));

        scheduler.reschedule(event);

        verify(repository).cancelById(eq(existing.getId()), any());
        verify(repository).save(any(EventReminder.class));
    }

    @Test
    void reschedule_cancelsExistingButDoesNotCreateWhenEventNoLongerEligible() throws Exception {
        event.setStartAt(LocalDateTime.now().plusHours(10));
        EventReminder existing = new EventReminder();
        setId(existing, UUID.randomUUID());
        existing.setStatus(ReminderStatus.PENDING);
        when(repository.findByEventIdAndStatusIn(eq(event.getId()), anyList()))
                .thenReturn(List.of(existing));

        scheduler.reschedule(event);

        verify(repository).cancelById(eq(existing.getId()), any());
        verify(repository, never()).save(any());
    }

    @Test
    void schedule_doesNotCreateReminderWhenStartAtIsNull() {
        event.setStartAt(null);

        scheduler.schedule(event);

        verify(repository, never()).save(any());
    }

    // EventReminder.id has no public setter; set via reflection
    private static void setId(EventReminder reminder, UUID id) throws Exception {
        Field field = EventReminder.class.getDeclaredField("id");
        field.setAccessible(true);
        field.set(reminder, id);
    }
}
