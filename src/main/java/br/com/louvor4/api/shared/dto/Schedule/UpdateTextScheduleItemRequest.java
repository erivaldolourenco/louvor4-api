package br.com.louvor4.api.shared.dto.Schedule;

import jakarta.validation.constraints.NotBlank;

public record UpdateTextScheduleItemRequest(
        @NotBlank String title,
        String description
) {}
