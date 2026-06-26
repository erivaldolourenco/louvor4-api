package br.com.louvor4.api.services;

public interface SmsService {
    void sendPasswordResetCode(String to, String code);
}
