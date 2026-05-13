package br.com.louvor4.api.jobs;

import br.com.louvor4.api.services.EventReminderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class ReminderPollingJob {

    private static final Logger log = LoggerFactory.getLogger(ReminderPollingJob.class);

    private final EventReminderService eventReminderService;

    public ReminderPollingJob(EventReminderService eventReminderService) {
        this.eventReminderService = eventReminderService;
    }

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    public void run() {
        log.debug("Verificando lembretes pendentes...");
        eventReminderService.processDue();
    }
}
