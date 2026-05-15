package br.com.louvor4.api.shared.dto.Program;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReorderProgramRequest(
        @NotNull @NotEmpty List<UUID> orderedIds
) {}
