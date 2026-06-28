package br.com.louvor4.entitlement.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class        PlanLimitExceededException extends RuntimeException {

    public PlanLimitExceededException(String key, int limit) {
        super(limit == 0
                ? "Funcionalidade não disponível no seu plano: " + key
                : "Limite do plano atingido para: " + key + " (máximo: " + limit + ")");
    }
}
