package br.com.louvor4.api.shared.dto.authentication;

public record ForgotPasswordChannelsResponse(
        String maskedEmail,
        String maskedPhone
) {
}
