package br.com.louvor4.api.shared.dto.Schedule;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReorderScheduleRequest(
        @NotNull @NotEmpty List<UUID> orderedIds
) {}
