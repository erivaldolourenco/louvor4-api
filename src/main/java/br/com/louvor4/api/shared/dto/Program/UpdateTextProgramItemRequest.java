package br.com.louvor4.api.shared.dto.Program;

import jakarta.validation.constraints.NotBlank;

public record UpdateTextProgramItemRequest(
        @NotBlank String title,
        String description
) {}
