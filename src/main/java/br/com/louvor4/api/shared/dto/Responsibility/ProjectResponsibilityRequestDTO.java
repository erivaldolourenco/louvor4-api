package br.com.louvor4.api.shared.dto.Responsibility;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ProjectResponsibilityRequestDTO(
        @NotBlank(message = "Informe o nome da responsabilidade.")
        @Size(max = 80, message = "O nome deve ter no máximo 80 caracteres.")
        String name,
        @Size(max = 500, message = "A descrição deve ter no máximo 500 caracteres.")
        String description
) {
}
