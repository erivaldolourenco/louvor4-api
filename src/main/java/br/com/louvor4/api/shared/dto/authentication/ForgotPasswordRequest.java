package br.com.louvor4.api.shared.dto.authentication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ForgotPasswordRequest(
        @NotBlank String identifier,
        @NotNull ResetChannel channel
) {
}
