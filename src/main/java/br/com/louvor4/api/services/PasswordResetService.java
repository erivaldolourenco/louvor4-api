package br.com.louvor4.api.services;

import br.com.louvor4.api.shared.dto.authentication.ForgotPasswordChannelsResponse;
import br.com.louvor4.api.shared.dto.authentication.ResetChannel;
import br.com.louvor4.api.shared.dto.authentication.ResetPasswordRequest;

public interface PasswordResetService {
    ForgotPasswordChannelsResponse getAvailableChannels(String identifier);
    void generateAndSendToken(String identifier, ResetChannel channel);
    void validateAndResetPassword(ResetPasswordRequest request);
}
