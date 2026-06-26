package br.com.louvor4.api.shared.dto.authentication;

import jakarta.validation.constraints.NotBlank;

public record ForgotPasswordChannelsRequest(
        @NotBlank String identifier
) {
}
