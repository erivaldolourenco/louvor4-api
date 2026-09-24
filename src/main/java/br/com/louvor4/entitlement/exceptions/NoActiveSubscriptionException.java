package br.com.louvor4.entitlement.exceptions;

import java.util.UUID;

public class NoActiveSubscriptionException extends RuntimeException {

    private final UUID userId;

    public NoActiveSubscriptionException(UUID userId) {
        super("Nenhuma assinatura ativa encontrada para o usuário: " + userId);
        this.userId = userId;
    }

    public UUID getUserId() {
        return userId;
    }

    public static String buildUserMessage(String username) {
        String target = (username == null || username.isBlank()) ? "na sua conta" : "para conta " + username;
        return "Não encontramos um plano ativo " + target + ". Entre em contato com o suporte para regularizar o seu acesso.";
    }
}
