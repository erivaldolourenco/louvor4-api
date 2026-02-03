package br.com.louvor4.api.shared.dto.eventOverview;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record MonthEventItem(UUID eventId,
                             String eventName,
                             LocalDate day,
                             LocalTime time,
                             List<MonthEventParticipantItem> participants,
                             List<MonthEventSongItem> songs) {
}
