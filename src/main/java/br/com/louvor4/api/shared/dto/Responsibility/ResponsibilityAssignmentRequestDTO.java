package br.com.louvor4.api.shared.dto.Responsibility;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record ResponsibilityAssignmentRequestDTO(
        @NotNull(message = "Informe o membro responsável.")
        UUID memberId,
        @NotNull(message = "Informe a data de início.")
        LocalDate startDate,
        @NotNull(message = "Informe a data de fim.")
        LocalDate endDate
) {
}
