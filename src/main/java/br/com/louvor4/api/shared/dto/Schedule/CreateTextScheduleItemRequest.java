package br.com.louvor4.api.shared.dto.Schedule;

import jakarta.validation.constraints.NotBlank;

public record CreateTextScheduleItemRequest(
        @NotBlank String title,
        String description
) {}
