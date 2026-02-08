package br.com.louvor4.api.shared.dto.authentication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(@NotBlank String email,
                                   @NotBlank @Size(min = 6, max = 6) String code,
                                   @NotBlank @Size(min = 6) String newPassword) {
}
