package br.com.louvor4.api.shared.dto.Schedule;

import br.com.louvor4.api.enums.ScheduleItemType;

import java.util.UUID;

public record ScheduleItemResponse(
        UUID id,
        ScheduleItemType type,
        Integer position,
        String title,
        String description,
        ScheduleMusicResponse music
) {}
