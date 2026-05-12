package br.com.louvor4.api.shared.dto.Schedule;

import java.util.UUID;

public record ScheduleMusicResponse(
        UUID id,
        String title,
        String artist
) {}
