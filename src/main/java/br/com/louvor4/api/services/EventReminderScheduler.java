package br.com.louvor4.api.services;

import br.com.louvor4.api.models.Event;

import java.util.UUID;

public interface EventReminderScheduler {
    void schedule(Event event);
    void reschedule(Event event);
    void cancel(UUID eventId);
}
