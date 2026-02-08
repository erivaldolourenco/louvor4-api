package br.com.louvor4.api.services;

import br.com.louvor4.api.shared.dto.authentication.ResetPasswordRequest;

public interface PasswordResetService {
    void generateAndSendToken(String email);
    void validateAndResetPassword(ResetPasswordRequest request);
}
