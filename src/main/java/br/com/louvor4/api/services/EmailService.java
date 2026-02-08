package br.com.louvor4.api.services;

public interface EmailService {
    void sendPasswordResetCode(String to, String code);
}
