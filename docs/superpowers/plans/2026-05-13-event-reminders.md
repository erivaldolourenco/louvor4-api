# Event Reminders Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Enviar lembretes automáticos 24h antes de cada evento para participantes ACCEPTED (lembrete) e PENDING (alerta de confirmação), via push (Firebase), notificação interna e e-mail (Brevo).

**Architecture:** Tabela `event_reminders` persiste o estado dos lembretes (PENDING → PROCESSING → SENT/FAILED/CANCELLED). Um `@Scheduled` job roda a cada 5 minutos chamando `EventReminderService.processDue()`, que é o único lugar com lógica de disparo. `EventReminderScheduler` gerencia o ciclo de vida dos registros e é chamado pelo `EventService` e `MusicProjectServiceImpl` nos eventos de criar/atualizar/deletar.

**Tech Stack:** Spring Boot, Spring Data JPA, Spring Scheduling, Firebase Admin SDK (já presente), Brevo via REST (já presente), JUnit 5 + Mockito

---

## Mapa de arquivos

| Ação | Arquivo |
|---|---|
| Criar | `src/main/java/br/com/louvor4/api/enums/ReminderStatus.java` |
| Criar | `src/main/java/br/com/louvor4/api/models/EventReminder.java` |
| Criar | `src/main/java/br/com/louvor4/api/repositories/EventReminderRepository.java` |
| Criar | `src/main/java/br/com/louvor4/api/services/EventReminderScheduler.java` |
| Criar | `src/main/java/br/com/louvor4/api/services/impl/EventReminderSchedulerImpl.java` |
| Criar | `src/main/java/br/com/louvor4/api/services/EventReminderService.java` |
| Criar | `src/main/java/br/com/louvor4/api/services/impl/EventReminderServiceImpl.java` |
| Criar | `src/main/java/br/com/louvor4/api/jobs/ReminderPollingJob.java` |
| Modificar | `src/main/java/br/com/louvor4/api/Louvor4ApiApplication.java` |
| Modificar | `src/main/java/br/com/louvor4/api/services/impl/MusicProjectServiceImpl.java` |
| Modificar | `src/main/java/br/com/louvor4/api/services/impl/EventServiceImpl.java` |
| Criar | `src/test/java/br/com/louvor4/api/services/impl/EventReminderSchedulerImplTest.java` |
| Criar | `src/test/java/br/com/louvor4/api/services/impl/EventReminderServiceImplTest.java` |

---

## Task 1: ReminderStatus enum

**Files:**
- Create: `src/main/java/br/com/louvor4/api/enums/ReminderStatus.java`

- [ ] **Criar o enum**

```java
package br.com.louvor4.api.enums;

public enum ReminderStatus {
    PENDING,
    PROCESSING,
    SENT,
    FAILED,
    CANCELLED
}
```

- [ ] **Compilar para verificar**

```bash
mvn compile -q
```
Esperado: sem erros.

- [ ] **Commit**

```bash
git add src/main/java/br/com/louvor4/api/enums/ReminderStatus.java
git commit -m "feat: adiciona enum ReminderStatus para lembretes de eventos"
```

---

## Task 2: EventReminder entity + EventReminderRepository

**Files:**
- Create: `src/main/java/br/com/louvor4/api/models/EventReminder.java`
- Create: `src/main/java/br/com/louvor4/api/repositories/EventReminderRepository.java`

- [ ] **Criar a entidade EventReminder**

```java
package br.com.louvor4.api.models;

import br.com.louvor4.api.enums.ReminderStatus;
import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "event_reminders",
    indexes = {
        @Index(name = "idx_event_reminders_due", columnList = "status, scheduled_for")
    }
)
public class EventReminder {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, columnDefinition = "uuid")
    private Event event;

    @Column(name = "scheduled_for", nullable = false)
    private LocalDateTime scheduledFor;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReminderStatus status = ReminderStatus.PENDING;

    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = ReminderStatus.PENDING;
        }
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public UUID getId() { return id; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public LocalDateTime getScheduledFor() { return scheduledFor; }
    public void setScheduledFor(LocalDateTime scheduledFor) { this.scheduledFor = scheduledFor; }
    public ReminderStatus getStatus() { return status; }
    public void setStatus(ReminderStatus status) { this.status = status; }
    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Criar o repositório**

```java
package br.com.louvor4.api.repositories;

import br.com.louvor4.api.enums.ReminderStatus;
import br.com.louvor4.api.models.EventReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface EventReminderRepository extends JpaRepository<EventReminder, UUID> {

    List<EventReminder> findByStatusAndScheduledForLessThanEqual(
            ReminderStatus status, LocalDateTime now);

    List<EventReminder> findByEventIdAndStatusIn(UUID eventId, List<ReminderStatus> statuses);

    List<EventReminder> findByStatusAndUpdatedAtBefore(
            ReminderStatus status, LocalDateTime threshold);

    @Modifying
    @Transactional
    @Query("""
        UPDATE EventReminder e
           SET e.status = :newStatus, e.updatedAt = :now
         WHERE e.id = :id AND e.status = :currentStatus
        """)
    int tryUpdateStatus(
            @Param("id") UUID id,
            @Param("currentStatus") ReminderStatus currentStatus,
            @Param("newStatus") ReminderStatus newStatus,
            @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("""
        UPDATE EventReminder e
           SET e.status = 'SENT', e.sentAt = :now, e.updatedAt = :now
         WHERE e.id = :id
        """)
    void markAsSent(@Param("id") UUID id, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("""
        UPDATE EventReminder e
           SET e.status = 'FAILED', e.errorMessage = :msg, e.updatedAt = :now
         WHERE e.id = :id
        """)
    void markAsFailed(@Param("id") UUID id, @Param("msg") String msg, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("""
        UPDATE EventReminder e
           SET e.status = 'CANCELLED', e.updatedAt = :now
         WHERE e.id = :id AND e.status IN ('PENDING', 'PROCESSING')
        """)
    int cancelById(@Param("id") UUID id, @Param("now") LocalDateTime now);
}
```

- [ ] **Compilar**

```bash
mvn compile -q
```
Esperado: sem erros.

- [ ] **Commit**

```bash
git add src/main/java/br/com/louvor4/api/models/EventReminder.java \
        src/main/java/br/com/louvor4/api/repositories/EventReminderRepository.java
git commit -m "feat: adiciona entidade EventReminder e repositório"
```

---

## Task 3: EventReminderScheduler + testes

**Files:**
- Create: `src/main/java/br/com/louvor4/api/services/EventReminderScheduler.java`
- Create: `src/main/java/br/com/louvor4/api/services/impl/EventReminderSchedulerImpl.java`
- Create: `src/test/java/br/com/louvor4/api/services/impl/EventReminderSchedulerImplTest.java`

- [ ] **Escrever os testes primeiro**

```java
package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.ReminderStatus;
import br.com.louvor4.api.models.Event;
import br.com.louvor4.api.models.EventReminder;
import br.com.louvor4.api.repositories.EventReminderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventReminderSchedulerImplTest {

    @Mock EventReminderRepository repository;
    @InjectMocks EventReminderSchedulerImpl scheduler;

    private Event event;

    @BeforeEach
    void setUp() {
        event = new Event();
        event.setId(UUID.randomUUID());
    }

    @Test
    void schedule_createsReminderWhen25hBefore() {
        event.setStartAt(LocalDateTime.now().plusHours(25));

        scheduler.schedule(event);

        ArgumentCaptor<EventReminder> captor = ArgumentCaptor.forClass(EventReminder.class);
        verify(repository).save(captor.capture());
        EventReminder saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(ReminderStatus.PENDING);
        assertThat(saved.getScheduledFor())
                .isCloseTo(event.getStartAt().minusHours(24), within(5, java.time.temporal.ChronoUnit.SECONDS));
    }

    @Test
    void schedule_doesNotCreateReminderWhen23hBefore() {
        event.setStartAt(LocalDateTime.now().plusHours(23));

        scheduler.schedule(event);

        verify(repository, never()).save(any());
    }

    @Test
    void schedule_doesNotCreateReminderWhenExactly24h() {
        event.setStartAt(LocalDateTime.now().plusHours(24));

        scheduler.schedule(event);

        verify(repository, never()).save(any());
    }

    @Test
    void cancel_cancelsExistingPendingReminder() {
        UUID eventId = event.getId();
        EventReminder reminder = new EventReminder();
        reminder.setId(UUID.randomUUID());
        reminder.setStatus(ReminderStatus.PENDING);
        when(repository.findByEventIdAndStatusIn(eq(eventId), anyList()))
                .thenReturn(List.of(reminder));

        scheduler.cancel(eventId);

        verify(repository).cancelById(eq(reminder.getId()), any(LocalDateTime.class));
    }

    @Test
    void cancel_doesNothingWhenNoReminderExists() {
        UUID eventId = event.getId();
        when(repository.findByEventIdAndStatusIn(eq(eventId), anyList()))
                .thenReturn(List.of());

        scheduler.cancel(eventId);

        verify(repository, never()).cancelById(any(), any());
    }

    @Test
    void reschedule_cancelsPreviousAndCreatesNew() {
        event.setStartAt(LocalDateTime.now().plusHours(30));
        EventReminder existing = new EventReminder();
        existing.setId(UUID.randomUUID());
        existing.setStatus(ReminderStatus.PENDING);
        when(repository.findByEventIdAndStatusIn(eq(event.getId()), anyList()))
                .thenReturn(List.of(existing));

        scheduler.reschedule(event);

        verify(repository).cancelById(eq(existing.getId()), any());
        verify(repository).save(any(EventReminder.class));
    }
}
```

- [ ] **Rodar testes e confirmar que falham**

```bash
mvn test -pl . -Dtest=EventReminderSchedulerImplTest -q 2>&1 | tail -10
```
Esperado: `ClassNotFoundException` ou `FAILED` — EventReminderSchedulerImpl não existe ainda.

- [ ] **Criar a interface**

```java
package br.com.louvor4.api.services;

import br.com.louvor4.api.models.Event;
import java.util.UUID;

public interface EventReminderScheduler {
    void schedule(Event event);
    void reschedule(Event event);
    void cancel(UUID eventId);
}
```

- [ ] **Criar a implementação**

```java
package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.ReminderStatus;
import br.com.louvor4.api.models.Event;
import br.com.louvor4.api.models.EventReminder;
import br.com.louvor4.api.repositories.EventReminderRepository;
import br.com.louvor4.api.services.EventReminderScheduler;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EventReminderSchedulerImpl implements EventReminderScheduler {

    private static final long REMINDER_HOURS_BEFORE = 24L;

    private final EventReminderRepository repository;

    public EventReminderSchedulerImpl(EventReminderRepository repository) {
        this.repository = repository;
    }

    @Override
    public void schedule(Event event) {
        if (!isEligible(event)) return;

        EventReminder reminder = new EventReminder();
        reminder.setEvent(event);
        reminder.setScheduledFor(event.getStartAt().minusHours(REMINDER_HOURS_BEFORE));
        reminder.setStatus(ReminderStatus.PENDING);
        repository.save(reminder);
    }

    @Override
    public void reschedule(Event event) {
        cancelExisting(event.getId());
        schedule(event);
    }

    @Override
    public void cancel(UUID eventId) {
        cancelExisting(eventId);
    }

    private boolean isEligible(Event event) {
        return event.getStartAt() != null &&
               event.getStartAt().isAfter(LocalDateTime.now().plusHours(REMINDER_HOURS_BEFORE));
    }

    private void cancelExisting(UUID eventId) {
        List<EventReminder> active = repository.findByEventIdAndStatusIn(
                eventId,
                List.of(ReminderStatus.PENDING, ReminderStatus.PROCESSING)
        );
        LocalDateTime now = LocalDateTime.now();
        active.forEach(r -> repository.cancelById(r.getId(), now));
    }
}
```

- [ ] **Rodar os testes e confirmar que passam**

```bash
mvn test -pl . -Dtest=EventReminderSchedulerImplTest -q 2>&1 | tail -10
```
Esperado: `BUILD SUCCESS`, todos os testes passando.

- [ ] **Commit**

```bash
git add src/main/java/br/com/louvor4/api/services/EventReminderScheduler.java \
        src/main/java/br/com/louvor4/api/services/impl/EventReminderSchedulerImpl.java \
        src/test/java/br/com/louvor4/api/services/impl/EventReminderSchedulerImplTest.java
git commit -m "feat: adiciona EventReminderScheduler para gerenciar ciclo de vida dos lembretes"
```

---

## Task 4: EventReminderService (processDue) + testes

**Files:**
- Create: `src/main/java/br/com/louvor4/api/services/EventReminderService.java`
- Create: `src/main/java/br/com/louvor4/api/services/impl/EventReminderServiceImpl.java`
- Create: `src/test/java/br/com/louvor4/api/services/impl/EventReminderServiceImplTest.java`

- [ ] **Escrever os testes**

```java
package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.EventParticipantStatus;
import br.com.louvor4.api.enums.ReminderStatus;
import br.com.louvor4.api.models.*;
import br.com.louvor4.api.repositories.EventParticipantRepository;
import br.com.louvor4.api.repositories.EventReminderRepository;
import br.com.louvor4.api.services.EmailService;
import br.com.louvor4.api.services.PushSenderService;
import br.com.louvor4.api.services.UserNotificationService;
import com.google.firebase.messaging.FirebaseMessagingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventReminderServiceImplTest {

    @Mock EventReminderRepository reminderRepository;
    @Mock EventParticipantRepository participantRepository;
    @Mock PushSenderService pushSenderService;
    @Mock UserNotificationService userNotificationService;
    @Mock EmailService emailService;
    @InjectMocks EventReminderServiceImpl service;

    private EventReminder reminder;
    private Event event;
    private User user;
    private EventParticipant acceptedParticipant;
    private EventParticipant pendingParticipant;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(UUID.randomUUID());
        user.setEmail("user@test.com");
        user.setFirstName("João");

        MusicProjectMember member = new MusicProjectMember();
        member.setUser(user);

        event = new Event();
        event.setId(UUID.randomUUID());
        event.setTitle("Culto Domingo");
        event.setStartAt(LocalDateTime.now().plusHours(1));

        acceptedParticipant = new EventParticipant();
        acceptedParticipant.setEvent(event);
        acceptedParticipant.setMember(member);
        acceptedParticipant.setStatus(EventParticipantStatus.ACCEPTED);

        pendingParticipant = new EventParticipant();
        pendingParticipant.setEvent(event);
        pendingParticipant.setMember(member);
        pendingParticipant.setStatus(EventParticipantStatus.PENDING);

        reminder = new EventReminder();
        reminder.setId(UUID.randomUUID());
        reminder.setEvent(event);
        reminder.setScheduledFor(LocalDateTime.now().minusMinutes(1));
        reminder.setStatus(ReminderStatus.PENDING);
    }

    @Test
    void processDue_skipsWhenAnotherPodClaimedReminder() {
        when(reminderRepository.findByStatusAndScheduledForLessThanEqual(
                eq(ReminderStatus.PENDING), any()))
                .thenReturn(List.of(reminder));
        when(reminderRepository.tryUpdateStatus(
                eq(reminder.getId()), eq(ReminderStatus.PENDING),
                eq(ReminderStatus.PROCESSING), any()))
                .thenReturn(0);

        service.processDue();

        verify(participantRepository, never()).findByEventId(any());
    }

    @Test
    void processDue_sendsToAcceptedAndPendingParticipants() throws FirebaseMessagingException {
        when(reminderRepository.findByStatusAndScheduledForLessThanEqual(
                eq(ReminderStatus.PENDING), any()))
                .thenReturn(List.of(reminder));
        when(reminderRepository.tryUpdateStatus(any(), any(), any(), any())).thenReturn(1);
        when(participantRepository.findByEventId(event.getId()))
                .thenReturn(List.of(acceptedParticipant, pendingParticipant));

        service.processDue();

        verify(pushSenderService, times(2)).sendToUser(eq(user.getId()), anyString(), anyString());
        verify(userNotificationService, times(2)).createNotification(any());
        verify(reminderRepository).markAsSent(eq(reminder.getId()), any());
    }

    @Test
    void processDue_marksFailedWhenPushThrows() throws FirebaseMessagingException {
        when(reminderRepository.findByStatusAndScheduledForLessThanEqual(
                eq(ReminderStatus.PENDING), any()))
                .thenReturn(List.of(reminder));
        when(reminderRepository.tryUpdateStatus(any(), any(), any(), any())).thenReturn(1);
        when(participantRepository.findByEventId(event.getId()))
                .thenReturn(List.of(acceptedParticipant));
        doThrow(new RuntimeException("Firebase down"))
                .when(pushSenderService).sendToUser(any(), anyString(), anyString());

        service.processDue();

        verify(reminderRepository).markAsFailed(eq(reminder.getId()), contains("Firebase down"), any());
    }

    @Test
    void processDue_recoversStuckProcessingReminders() {
        reminder.setStatus(ReminderStatus.PROCESSING);
        when(reminderRepository.findByStatusAndScheduledForLessThanEqual(
                eq(ReminderStatus.PENDING), any()))
                .thenReturn(List.of());
        when(reminderRepository.findByStatusAndUpdatedAtBefore(
                eq(ReminderStatus.PROCESSING), any()))
                .thenReturn(List.of(reminder));

        service.processDue();

        verify(reminderRepository).markAsFailed(eq(reminder.getId()), anyString(), any());
    }
}
```

- [ ] **Rodar testes e confirmar que falham**

```bash
mvn test -pl . -Dtest=EventReminderServiceImplTest -q 2>&1 | tail -10
```
Esperado: `FAILED` — EventReminderServiceImpl não existe ainda.

- [ ] **Criar a interface**

```java
package br.com.louvor4.api.services;

public interface EventReminderService {
    void processDue();
}
```

- [ ] **Criar a implementação**

```java
package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.EventParticipantStatus;
import br.com.louvor4.api.enums.NotificationType;
import br.com.louvor4.api.enums.ReminderStatus;
import br.com.louvor4.api.models.EventParticipant;
import br.com.louvor4.api.models.EventReminder;
import br.com.louvor4.api.repositories.EventParticipantRepository;
import br.com.louvor4.api.repositories.EventReminderRepository;
import br.com.louvor4.api.services.EmailService;
import br.com.louvor4.api.services.EventReminderService;
import br.com.louvor4.api.services.PushSenderService;
import br.com.louvor4.api.services.UserNotificationService;
import br.com.louvor4.api.shared.dto.notification.CreateUserNotificationRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class EventReminderServiceImpl implements EventReminderService {

    private static final Logger log = LoggerFactory.getLogger(EventReminderServiceImpl.class);
    private static final int STUCK_PROCESSING_MINUTES = 10;

    private final EventReminderRepository reminderRepository;
    private final EventParticipantRepository participantRepository;
    private final PushSenderService pushSenderService;
    private final UserNotificationService userNotificationService;
    private final EmailService emailService;

    public EventReminderServiceImpl(
            EventReminderRepository reminderRepository,
            EventParticipantRepository participantRepository,
            PushSenderService pushSenderService,
            UserNotificationService userNotificationService,
            EmailService emailService) {
        this.reminderRepository = reminderRepository;
        this.participantRepository = participantRepository;
        this.pushSenderService = pushSenderService;
        this.userNotificationService = userNotificationService;
        this.emailService = emailService;
    }

    @Override
    public void processDue() {
        recoverStuck();

        LocalDateTime now = LocalDateTime.now();
        List<EventReminder> due = reminderRepository
                .findByStatusAndScheduledForLessThanEqual(ReminderStatus.PENDING, now);

        for (EventReminder reminder : due) {
            int claimed = reminderRepository.tryUpdateStatus(
                    reminder.getId(), ReminderStatus.PENDING, ReminderStatus.PROCESSING, now);
            if (claimed == 0) continue;

            try {
                sendForReminder(reminder);
                reminderRepository.markAsSent(reminder.getId(), LocalDateTime.now());
            } catch (Exception e) {
                log.error("Falha ao processar lembrete {}: {}", reminder.getId(), e.getMessage(), e);
                reminderRepository.markAsFailed(reminder.getId(), e.getMessage(), LocalDateTime.now());
            }
        }
    }

    private void recoverStuck() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(STUCK_PROCESSING_MINUTES);
        List<EventReminder> stuck = reminderRepository
                .findByStatusAndUpdatedAtBefore(ReminderStatus.PROCESSING, threshold);
        LocalDateTime now = LocalDateTime.now();
        stuck.forEach(r -> reminderRepository.markAsFailed(r.getId(), "Stuck in PROCESSING", now));
    }

    private void sendForReminder(EventReminder reminder) {
        List<EventParticipant> participants =
                participantRepository.findByEventId(reminder.getEvent().getId());

        for (EventParticipant p : participants) {
            if (p.getStatus() == EventParticipantStatus.ACCEPTED) {
                sendReminderToAccepted(reminder, p);
            } else if (p.getStatus() == EventParticipantStatus.PENDING) {
                sendNudgeToPending(reminder, p);
            }
        }
    }

    private void sendReminderToAccepted(EventReminder reminder, EventParticipant participant) {
        UUID userId = participant.getMember().getUser().getId();
        String title = "Lembrete: " + reminder.getEvent().getTitle();
        String message = reminder.getEvent().getTitle() + " acontece amanhã. Até lá!";

        sendAll(userId, participant.getMember().getUser().getEmail(), title, message, reminder);
    }

    private void sendNudgeToPending(EventReminder reminder, EventParticipant participant) {
        UUID userId = participant.getMember().getUser().getId();
        String title = reminder.getEvent().getTitle() + " é amanhã";
        String message = "Você ainda não confirmou sua presença em "
                + reminder.getEvent().getTitle() + ". Acesse o app para responder.";

        sendAll(userId, participant.getMember().getUser().getEmail(), title, message, reminder);
    }

    private void sendAll(UUID userId, String email, String title, String message,
                         EventReminder reminder) {
        try {
            pushSenderService.sendToUser(userId, title, message);
        } catch (Exception e) {
            log.warn("Push falhou para usuário {}: {}", userId, e.getMessage());
        }

        try {
            userNotificationService.createNotification(new CreateUserNotificationRequest(
                    NotificationType.EVENT_REMINDER,
                    userId,
                    title,
                    message,
                    null,
                    null
            ));
        } catch (Exception e) {
            log.warn("Notificação interna falhou para usuário {}: {}", userId, e.getMessage());
        }

        try {
            emailService.sendEventReminder(email, title, message);
        } catch (Exception e) {
            log.warn("E-mail falhou para {}: {}", email, e.getMessage());
        }
    }
}
```

- [ ] **Adicionar `sendEventReminder` no `EmailService`**

Arquivo: `src/main/java/br/com/louvor4/api/services/EmailService.java`

Adicionar o método à interface existente:
```java
void sendEventReminder(String to, String subject, String message);
```

- [ ] **Implementar `sendEventReminder` no `BrevoEmailServiceImpl`**

Arquivo: `src/main/java/br/com/louvor4/api/services/impl/BrevoEmailServiceImpl.java`

Adicionar após o último método existente:
```java
@Override
public void sendEventReminder(String to, String subject, String message) {
    Map<String, Object> body = Map.of(
        "sender", Map.of("name", emailConfig.getSenderName(), "email", emailConfig.getSenderEmail()),
        "to", List.of(Map.of("email", to)),
        "subject", subject,
        "textContent", message
    );

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.set("api-key", apiKey);

    restTemplate.postForEntity(apiUrl, new HttpEntity<>(body, headers), String.class);
}
```

- [ ] **Rodar os testes e confirmar que passam**

```bash
mvn test -pl . -Dtest=EventReminderServiceImplTest -q 2>&1 | tail -10
```
Esperado: `BUILD SUCCESS`.

- [ ] **Commit**

```bash
git add src/main/java/br/com/louvor4/api/services/EventReminderService.java \
        src/main/java/br/com/louvor4/api/services/impl/EventReminderServiceImpl.java \
        src/main/java/br/com/louvor4/api/services/EmailService.java \
        src/main/java/br/com/louvor4/api/services/impl/BrevoEmailServiceImpl.java \
        src/test/java/br/com/louvor4/api/services/impl/EventReminderServiceImplTest.java
git commit -m "feat: adiciona EventReminderService com lógica de disparo de lembretes"
```

---

## Task 5: ReminderPollingJob + @EnableScheduling

**Files:**
- Create: `src/main/java/br/com/louvor4/api/jobs/ReminderPollingJob.java`
- Modify: `src/main/java/br/com/louvor4/api/Louvor4ApiApplication.java`

- [ ] **Adicionar `@EnableScheduling` na classe principal**

Arquivo: `src/main/java/br/com/louvor4/api/Louvor4ApiApplication.java`

Trocar:
```java
@EnableAsync
@ConfigurationPropertiesScan
@SpringBootApplication
public class Louvor4ApiApplication {
```
Por:
```java
@EnableAsync
@EnableScheduling
@ConfigurationPropertiesScan
@SpringBootApplication
public class Louvor4ApiApplication {
```

Adicionar o import:
```java
import org.springframework.scheduling.annotation.EnableScheduling;
```

- [ ] **Criar o job**

```java
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
```

- [ ] **Compilar**

```bash
mvn compile -q
```
Esperado: sem erros.

- [ ] **Commit**

```bash
git add src/main/java/br/com/louvor4/api/jobs/ReminderPollingJob.java \
        src/main/java/br/com/louvor4/api/Louvor4ApiApplication.java
git commit -m "feat: adiciona ReminderPollingJob e habilita @EnableScheduling"
```

---

## Task 6: Integrar EventReminderScheduler no fluxo de criação/atualização/deleção de eventos

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/services/impl/MusicProjectServiceImpl.java`
- Modify: `src/main/java/br/com/louvor4/api/services/impl/EventServiceImpl.java`

- [ ] **Injetar `EventReminderScheduler` em `MusicProjectServiceImpl`**

Localizar o campo:
```java
private final EventReminderScheduler eventReminderScheduler;
```

Adicionar ao construtor (junto com os outros parâmetros existentes) e ao corpo do construtor:
```java
this.eventReminderScheduler = eventReminderScheduler;
```

Adicionar o import:
```java
import br.com.louvor4.api.services.EventReminderScheduler;
```

- [ ] **Chamar `schedule` após criar evento em `MusicProjectServiceImpl`**

Localizar o método `createEvent`. Trocar:
```java
        Event saved = eventRepository.save(event);
        return eventMapper.toDto(saved);
```
Por:
```java
        Event saved = eventRepository.save(event);
        eventReminderScheduler.schedule(saved);
        return eventMapper.toDto(saved);
```

- [ ] **Injetar `EventReminderScheduler` em `EventServiceImpl`**

Adicionar o campo:
```java
private final EventReminderScheduler eventReminderScheduler;
```

Adicionar ao construtor existente como parâmetro e no corpo:
```java
this.eventReminderScheduler = eventReminderScheduler;
```

Adicionar o import:
```java
import br.com.louvor4.api.services.EventReminderScheduler;
```

- [ ] **Chamar `cancel` em `deleteEventById`**

Localizar `deleteEventById`. Adicionar antes de `eventRepository.delete(event)`:
```java
        eventReminderScheduler.cancel(eventId);
        eventRepository.delete(event);
```

- [ ] **Chamar `reschedule` em `updateEventBy`**

Localizar `updateEventBy`. Trocar:
```java
        eventMapper.updateEntityFromDto(eventDto, event);
        eventRepository.save(event);
```
Por:
```java
        eventMapper.updateEntityFromDto(eventDto, event);
        Event saved = eventRepository.save(event);
        eventReminderScheduler.reschedule(saved);
```

- [ ] **Compilar**

```bash
mvn compile -q
```
Esperado: sem erros.

- [ ] **Rodar toda a suite de testes**

```bash
mvn test -q 2>&1 | tail -15
```
Esperado: `BUILD SUCCESS`.

- [ ] **Commit**

```bash
git add src/main/java/br/com/louvor4/api/services/impl/MusicProjectServiceImpl.java \
        src/main/java/br/com/louvor4/api/services/impl/EventServiceImpl.java
git commit -m "feat: integra EventReminderScheduler ao ciclo de vida de criação, atualização e deleção de eventos"
```
