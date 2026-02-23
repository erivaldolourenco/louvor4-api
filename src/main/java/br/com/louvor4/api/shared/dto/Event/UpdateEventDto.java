package br.com.louvor4.api.shared.dto.Event;

public record UpdateEventDto(
        String title,
        String description,
        String startDate,
        String startTime,
        String location
) {
}
