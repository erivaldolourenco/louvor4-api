package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.models.Event;
import br.com.louvor4.api.models.EventParticipant;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Textos das notificações de evento (push e aviso). Padrão: emoji + rótulo +
 * título do evento no título; data ("Sáb, 26/12 às 19h") + frase curta no corpo.
 * <pre>
 * 🎵 Nova escala: Culto de Domingo
 * Sáb, 26/12 às 19h. Você foi escalado na função Vocal.
 * </pre>
 */
final class EventNotificationMessages {

    private static final DateTimeFormatter DAY_MONTH = DateTimeFormatter.ofPattern("dd/MM");

    private EventNotificationMessages() {
    }

    // Convite para escala

    static String inviteTitle(Event event) {
        return "🎵 Nova escala: " + eventTitle(event);
    }

    static String inviteBody(Event event, EventParticipant participant) {
        return when(event) + "Você foi escalado" + inSkill(participant) + ".";
    }

    // Repertório

    static String setlistTitle(Event event) {
        return "🎶 Repertório atualizado: " + eventTitle(event);
    }

    static String setlistBody(Event event, List<String> itemTitles, boolean added) {
        boolean plural = itemTitles.size() > 1;
        String label = added
                ? (plural ? "Músicas adicionadas: " : "Música adicionada: ")
                : (plural ? "Músicas removidas: " : "Música removida: ");
        return when(event) + label + String.join(", ", itemTitles) + ".";
    }

    // Resposta do participante (para o dono do projeto)

    static String acceptedTitle(Event event) {
        return "✅ Presença confirmada: " + eventTitle(event);
    }

    static String acceptedBody(Event event, EventParticipant participant, String participantName) {
        return when(event) + participantName + " confirmou presença" + inSkill(participant) + ".";
    }

    static String declinedTitle(Event event) {
        return "❌ Escala recusada: " + eventTitle(event);
    }

    static String declinedBody(Event event, EventParticipant participant, String participantName) {
        return when(event) + participantName + " não poderá participar" + inSkill(participant) + ".";
    }

    // Lembretes da véspera

    static String reminderTitle(Event event) {
        return "⏰ É amanhã: " + eventTitle(event);
    }

    static String reminderBody(Event event, EventParticipant participant) {
        return when(event) + "Você está escalado" + inSkill(participant) + ". Até lá!";
    }

    static String pendingReminderTitle(Event event) {
        return "⏳ Confirme sua presença: " + eventTitle(event);
    }

    static String pendingReminderBody(Event event) {
        return when(event) + "Você ainda não respondeu à escala.";
    }

    /** "Sáb, 26/12 às 19h" ou "Sáb, 26/12 às 19h30". */
    static String formatWhen(LocalDateTime dateTime) {
        if (dateTime == null) return "Data a definir";
        String hour = dateTime.getMinute() == 0
                ? dateTime.getHour() + "h"
                : dateTime.getHour() + "h" + String.format("%02d", dateTime.getMinute());
        return weekday(dateTime.getDayOfWeek()) + ", " + dateTime.format(DAY_MONTH) + " às " + hour;
    }

    private static String when(Event event) {
        return formatWhen(event.getStartAt()) + ". ";
    }

    /** " na função Vocal" ou "" quando o participante não tem função. */
    private static String inSkill(EventParticipant participant) {
        String skill = participant.getSkill() == null ? null : trimToNull(participant.getSkill().getName());
        return skill == null ? "" : " na função " + skill;
    }

    private static String eventTitle(Event event) {
        String title = trimToNull(event.getTitle());
        return title == null ? "Evento" : title;
    }

    private static String weekday(DayOfWeek day) {
        return switch (day) {
            case MONDAY -> "Seg";
            case TUESDAY -> "Ter";
            case WEDNESDAY -> "Qua";
            case THURSDAY -> "Qui";
            case FRIDAY -> "Sex";
            case SATURDAY -> "Sáb";
            case SUNDAY -> "Dom";
        };
    }

    private static String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
