package br.com.louvor4.api.controllers;

import br.com.louvor4.api.exceptions.ForbiddenException;
import br.com.louvor4.api.services.EventReminderService;
import br.com.louvor4.api.services.PushSenderService;
import br.com.louvor4.api.shared.dto.notification.TestPushNotificationRequest;
import com.google.firebase.messaging.FirebaseMessagingException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints chamados por um scheduler externo (cron), não por usuários do app.
 * Protegidos por um token compartilhado (X-Internal-Token) em vez de JWT de usuário,
 * já que quem dispara é infraestrutura, não uma sessão autenticada.
 * Retornam 202 antes do processamento terminar (processDue roda @Async) para
 * o scheduler externo não sofrer timeout em execuções longas.
 */
@RestController
@RequestMapping("internal/jobs")
public class InternalJobController {

    private final EventReminderService eventReminderService;
    private final PushSenderService pushSenderService;

    @Value("${app.internal.jobs-token}")
    private String jobsToken;

    public InternalJobController(EventReminderService eventReminderService, PushSenderService pushSenderService) {
        this.eventReminderService = eventReminderService;
        this.pushSenderService = pushSenderService;
    }

    @PostMapping("/event-reminders/process")
    public ResponseEntity<Void> processEventReminders(
            @RequestHeader(value = "X-Internal-Token", required = false) String token
    ) {
        validateToken(token);
        eventReminderService.processDue();
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    @PostMapping("/push-notifications/test")
    public ResponseEntity<Void> sendTestPushNotification(
            @RequestHeader(value = "X-Internal-Token", required = false) String token,
            @RequestBody @Valid TestPushNotificationRequest request
    ) throws FirebaseMessagingException {
        validateToken(token);
        pushSenderService.sendToUser(request.userId(), request.title(), request.message(), request.imageUrl());
        return ResponseEntity.status(HttpStatus.ACCEPTED).build();
    }

    private void validateToken(String token) {
        if (jobsToken == null || jobsToken.isBlank() || !jobsToken.equals(token)) {
            throw new ForbiddenException("Token interno inválido.");
        }
    }
}
