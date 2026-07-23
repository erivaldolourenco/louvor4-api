package br.com.louvor4.api.shared.dto.Event;

import java.util.List;

public record CreateEventBatchDto(
        String title,
        String description,
        List<String> dates,
        String startTime,
        String location
) {}
