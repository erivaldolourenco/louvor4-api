package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.ReminderStatus;
import br.com.louvor4.api.models.Event;
import br.com.louvor4.api.models.EventReminder;
import br.com.louvor4.api.repositories.EventReminderRepository;
import br.com.louvor4.api.services.EventReminderScheduler;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EventReminderSchedulerImpl implements EventReminderScheduler {

    private static final long REMINDER_HOURS_BEFORE = 24L;

    private final EventReminderRepository repository;

    public EventReminderSchedulerImpl(EventReminderRepository repository) {
        this.repository = repository;
    }

    @Override
    public void schedule(Event event) {
        if (!isEligible(event)) return;

        EventReminder reminder = new EventReminder();
        reminder.setEvent(event);
        reminder.setScheduledFor(event.getStartAt().minusHours(REMINDER_HOURS_BEFORE));
        reminder.setStatus(ReminderStatus.PENDING);
        repository.save(reminder);
    }

    @Override
    public void reschedule(Event event) {
        cancelExisting(event.getId());
        schedule(event);
    }

    @Override
    public void cancel(UUID eventId) {
        cancelExisting(eventId);
    }

    private boolean isEligible(Event event) {
        return event.getStartAt() != null &&
               event.getStartAt().isAfter(LocalDateTime.now().plusHours(REMINDER_HOURS_BEFORE));
    }

    private void cancelExisting(UUID eventId) {
        List<EventReminder> active = repository.findByEventIdAndStatusIn(
                eventId,
                List.of(ReminderStatus.PENDING, ReminderStatus.PROCESSING)
        );
        LocalDateTime now = LocalDateTime.now();
        active.forEach(r -> repository.cancelById(r.getId(), now));
    }
}
