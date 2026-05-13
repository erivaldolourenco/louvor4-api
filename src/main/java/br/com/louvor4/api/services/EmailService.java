package br.com.louvor4.api.services;

public interface EmailService {
    void sendPasswordResetCode(String to, String code);
    void sendEmailVerificationCode(String to, String code);
    void sendEventReminder(String to, String subject, String message);
}
