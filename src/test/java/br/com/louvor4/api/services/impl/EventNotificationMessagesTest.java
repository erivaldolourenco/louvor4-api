package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.models.Event;
import br.com.louvor4.api.models.EventParticipant;
import br.com.louvor4.api.models.ProjectSkill;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EventNotificationMessagesTest {

    private final Event event = event("Culto de Domingo", LocalDateTime.of(2026, 12, 26, 19, 0));

    @Test
    void invite_withSkill_ignoresLocation() {
        event.setLocation("Igreja Central");

        assertEquals("🎵 Nova escala: Culto de Domingo", EventNotificationMessages.inviteTitle(event));
        assertEquals(
                "Sáb, 26/12 às 19h. Você foi escalado na função Vocal.",
                EventNotificationMessages.inviteBody(event, participant("Vocal")));
    }

    @Test
    void invite_withoutSkill_andMinutes() {
        Event ensaio = event("Ensaio", LocalDateTime.of(2026, 12, 27, 9, 30));

        assertEquals("Dom, 27/12 às 9h30. Você foi escalado.",
                EventNotificationMessages.inviteBody(ensaio, participant(null)));
    }

    @Test
    void invite_withoutTitleNorDate() {
        Event empty = event(null, null);

        assertEquals("🎵 Nova escala: Evento", EventNotificationMessages.inviteTitle(empty));
        assertEquals("Data a definir. Você foi escalado.",
                EventNotificationMessages.inviteBody(empty, participant(null)));
    }

    @Test
    void setlist_singularAndPlural() {
        assertEquals("🎶 Repertório atualizado: Culto de Domingo", EventNotificationMessages.setlistTitle(event));
        assertEquals("Sáb, 26/12 às 19h. Música adicionada: Grande é o Senhor.",
                EventNotificationMessages.setlistBody(event, List.of("Grande é o Senhor"), true));
        assertEquals("Sáb, 26/12 às 19h. Músicas removidas: A, B.",
                EventNotificationMessages.setlistBody(event, List.of("A", "B"), false));
    }

    @Test
    void participantResponse() {
        assertEquals("✅ Presença confirmada: Culto de Domingo", EventNotificationMessages.acceptedTitle(event));
        assertEquals("Sáb, 26/12 às 19h. João confirmou presença na função Vocal.",
                EventNotificationMessages.acceptedBody(event, participant("Vocal"), "João"));
        assertEquals("❌ Escala recusada: Culto de Domingo", EventNotificationMessages.declinedTitle(event));
        assertEquals("Sáb, 26/12 às 19h. João não poderá participar.",
                EventNotificationMessages.declinedBody(event, participant(null), "João"));
    }

    @Test
    void reminders() {
        assertEquals("⏰ É amanhã: Culto de Domingo", EventNotificationMessages.reminderTitle(event));
        assertEquals("Sáb, 26/12 às 19h. Você está escalado. Até lá!",
                EventNotificationMessages.reminderBody(event, participant(null)));
        assertEquals("⏳ Confirme sua presença: Culto de Domingo", EventNotificationMessages.pendingReminderTitle(event));
        assertEquals("Sáb, 26/12 às 19h. Você ainda não respondeu à escala.",
                EventNotificationMessages.pendingReminderBody(event));
    }

    private Event event(String title, LocalDateTime startAt) {
        Event event = new Event();
        event.setTitle(title);
        event.setStartAt(startAt);
        return event;
    }

    private EventParticipant participant(String skillName) {
        EventParticipant participant = new EventParticipant();
        if (skillName != null) {
            ProjectSkill skill = new ProjectSkill();
            skill.setName(skillName);
            participant.setSkill(skill);
        }
        return participant;
    }
}
