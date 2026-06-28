package br.com.louvor4.entitlement.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Map;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class        PlanLimitExceededException extends RuntimeException {

    private static final Map<String, String> KEY_LABELS = Map.of(
            "max_songs",            "músicas cadastradas",
            "max_projects",         "projetos",
            "max_project_members",  "membros no projeto",
            "upload_audio",         "upload de áudio"
    );

    public PlanLimitExceededException(String key, int limit) {
        super(buildMessage(key, limit));
    }

    private static String buildMessage(String key, int limit) {
        String label = KEY_LABELS.getOrDefault(key, key);
        if (limit == 0) {
            return "O seu plano não inclui " + label + ". Faça upgrade para liberar esta funcionalidade.";
        }
        return "Você atingiu o limite de " + label + " do seu plano (máximo: " + limit + "). Faça upgrade para continuar.";
    }
}
