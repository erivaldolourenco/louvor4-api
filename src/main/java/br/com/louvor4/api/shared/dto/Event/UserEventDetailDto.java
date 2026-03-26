package br.com.louvor4.api.shared.dto.Event;

import br.com.louvor4.api.enums.EventParticipantStatus;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record UserEventDetailDto(
        UUID id,
        UUID projectId,
        String title,
        String description,
        LocalDate date,
        LocalTime time,
        String location,
        String projectTitle,
        String projectImageUrl,
        Integer participantsCount,
        Integer repertoireCount,
        List<String> participantsProfileImages,
        UUID participantId,
        EventParticipantStatus participantStatus
) {
}
