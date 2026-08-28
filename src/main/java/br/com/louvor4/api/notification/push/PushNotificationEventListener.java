package br.com.louvor4.api.notification.push;

import br.com.louvor4.api.services.PushSenderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class PushNotificationEventListener {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationEventListener.class);

    private final PushSenderService pushSenderService;

    public PushNotificationEventListener(PushSenderService pushSenderService) {
        this.pushSenderService = pushSenderService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onPushNotification(PushNotificationEvent event) {
        try {
            pushSenderService.sendToUser(event.userId(), event.title(), event.message());
        } catch (Exception e) {
            log.warn("Falha ao enviar push para usuário {}: {}", event.userId(), e.getMessage());
        }
    }
}
