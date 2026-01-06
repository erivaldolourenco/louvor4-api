package br.com.louvor4.api.shared.dto.Event;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateEventDto(
        UUID id,
        UUID projectId,
        String title,
        String description,
        String startDate,
        String startTime,
        String location
) {}
