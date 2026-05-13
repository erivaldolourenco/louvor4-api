# Design: Lembretes Agendados de Eventos

**Data:** 2026-05-13
**Status:** Aprovado

---

## Contexto

O sistema precisa notificar participantes de eventos 24h antes do início. A notificação é enviada por três canais simultaneamente: push notification (Firebase), notificação interna (sistema existente) e e-mail (Brevo). Dois tipos de mensagem distintos são enviados conforme o status do participante.

---

## Requisitos

- Disparar lembretes 24h antes de `event.startAt`
- Não agendar se o evento for criado com ≤ 24h de antecedência
- Participantes `ACCEPTED`: mensagem de lembrete do evento
- Participantes `PENDING`: mensagem alertando que ainda não confirmaram presença
- Canais: push (Firebase) + notificação interna + e-mail (Brevo)
- Sobreviver a restarts do servidor sem perda de lembretes
- Funcionar com múltiplas instâncias da API rodando em paralelo

---

## Arquitetura

Dois fluxos independentes conectados pela tabela `event_reminders`:

### Fluxo 1 — Agendamento

Disparado pelo `EventService` nos eventos de criação, atualização e cancelamento.

```
EventService.createEvent()  ──→ EventReminderScheduler.schedule()
EventService.updateEvent()  ──→ EventReminderScheduler.reschedule()
EventService.cancelEvent()  ──→ EventReminderScheduler.cancel()
                                       └→ event_reminders (tabela)
```

### Fluxo 2 — Disparo (a cada 5 minutos)

```
ReminderPollingJob (@Scheduled)
  └→ EventReminderService.processDue()
        ├→ busca registros PENDING com scheduled_for <= agora
        ├→ marca cada um como PROCESSING
        ├→ busca participantes do evento
        ├→ ACCEPTED → envia lembrete
        ├→ PENDING  → envia alerta de confirmação pendente
        ├→ PushSenderService       (Firebase)
        ├→ UserNotificationService (notificação interna)
        └→ EmailService            (Brevo)
              └→ marca como SENT ou FAILED
```

---

## Modelo de dados

```sql
CREATE TABLE event_reminders (
    id            UUID        PRIMARY KEY,
    event_id      UUID        NOT NULL REFERENCES events(id) ON DELETE CASCADE,
    scheduled_for TIMESTAMP   NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    sent_at       TIMESTAMP,
    error_message TEXT,
    created_at    TIMESTAMP   NOT NULL,
    updated_at    TIMESTAMP   NOT NULL
);

CREATE INDEX idx_event_reminders_due
    ON event_reminders(status, scheduled_for)
    WHERE status = 'PENDING';
```

### Ciclo de vida do status

```
PENDING → PROCESSING → SENT
                     → FAILED
         → CANCELLED  (antes do disparo)
```

| Status | Significado |
|---|---|
| `PENDING` | Aguardando disparo |
| `PROCESSING` | Em processamento — protege contra duplicatas em crash |
| `SENT` | Enviado com sucesso em todos os canais |
| `FAILED` | Falhou — detalhes em `error_message` |
| `CANCELLED` | Evento cancelado ou reagendado antes do disparo |

---

## Componentes

### `EventReminder` — entidade JPA
Mapeia a tabela `event_reminders`. Sem lógica de negócio.

### `EventReminderRepository` — repositório
```java
List<EventReminder> findByStatusAndScheduledForLessThanEqual(
    ReminderStatus status, LocalDateTime now);

Optional<EventReminder> findByEventIdAndStatus(
    UUID eventId, ReminderStatus status);

// Para recuperação de PROCESSING travados
List<EventReminder> findByStatusAndUpdatedAtBefore(
    ReminderStatus status, LocalDateTime threshold);
```

### `EventReminderScheduler` — gerencia ciclo de vida
```java
void schedule(Event event);    // cria PENDING se startAt - agora > 24h
void reschedule(Event event);  // cancela PENDING existente + schedule()
void cancel(UUID eventId);     // PENDING/PROCESSING → CANCELLED
```

### `EventReminderService` — lógica de disparo
```java
void processDue();  // busca PENDING vencidos, envia, marca SENT/FAILED
```
Orquestra os três canais de notificação. Cada lembrete é processado em transação independente — falha em um não afeta os outros.

### `ReminderPollingJob` — gatilho
```java
@Scheduled(fixedDelay = 5, timeUnit = MINUTES)
public void run() {
    eventReminderService.processDue();
}
```
Classe mínima, isolada. Para migrar para Temporal, Airflow ou qualquer scheduler externo: deletar esta classe, expor `POST /internal/reminders/process`. Nenhuma lógica de negócio muda.

---

## Tratamento de erros e casos de borda

### Falha de canal (Firebase/Brevo fora do ar)
Cada canal é tentado independentemente. Se o e-mail falhar mas push e notificação interna tiverem sucesso, o lembrete é marcado `FAILED` com `error_message` descrevendo o canal que falhou. Status `FAILED` é terminal — sem retry automático por ora. Evita spam em caso de falha recorrente.

### Crash durante processamento
O status `PROCESSING` previne reprocessamento duplicado. Se o servidor cair com registros presos em `PROCESSING`, o job de recuperação (rodando junto com `processDue()`) reprocessa registros `PROCESSING` com `updated_at` anterior a 10 minutos.

### Concorrência entre múltiplas instâncias
A query usa `SELECT ... FOR UPDATE SKIP LOCKED`, garantindo que dois pods nunca processem o mesmo lembrete simultaneamente. Sem Redis ou coordenação externa.

### Evento atualizado com nova data

| Cenário | Ação |
|---|---|
| Nova data ainda > 24h | Cancela PENDING antigo, cria novo PENDING |
| Nova data ≤ 24h de antecedência | Cancela PENDING, não cria novo |
| Lembrete já SENT | Não faz nada — histórico preservado |

### Evento cancelado
`cancel()` age apenas em `PENDING` e `PROCESSING`. Registros `SENT` e `FAILED` são histórico imutável.

---

## Mensagens enviadas

### Para participantes ACCEPTED
- **Push:** "Lembrete: [Nome do Evento] acontece amanhã"
- **Notificação interna:** tipo `EVENT_REMINDER`, mesma mensagem
- **E-mail:** título + data + local do evento

### Para participantes PENDING
- **Push:** "[Nome do Evento] é amanhã e você ainda não confirmou presença"
- **Notificação interna:** tipo `EVENT_REMINDER`, mesma mensagem
- **E-mail:** link/instrução para confirmar presença

---

## Path de migração para scheduler externo

A separação entre `ReminderPollingJob` (gatilho) e `EventReminderService` (lógica) garante que migrar para Temporal.io, Apache Airflow ou qualquer outro scheduler é uma operação cirúrgica:

1. Remover `ReminderPollingJob`
2. Expor `POST /internal/reminders/process` que chama `eventReminderService.processDue()`
3. Configurar o scheduler externo para chamar esse endpoint

Nenhuma linha de lógica de negócio precisa mudar.
