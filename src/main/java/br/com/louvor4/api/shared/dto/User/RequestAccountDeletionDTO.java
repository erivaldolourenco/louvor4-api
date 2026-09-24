package br.com.louvor4.api.shared.dto.User;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequestAccountDeletionDTO(
        @NotBlank @Email String email
) {
}
