# Event Schedule (Programação) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar a funcionalidade de Programação do Evento — uma lista ordenada de itens (músicas do repertório e textos livres) que representa a ordem do culto.

**Architecture:** Nova entidade `EventScheduleItem` com dois tipos (MUSIC/TEXT), acompanhada de repositório, service e controller próprios. O `EventServiceImpl` chama hooks internos do `ScheduleService` ao adicionar/remover músicas do repertório, mantendo a programação sincronizada automaticamente.

**Tech Stack:** Spring Boot 3.5.0, Java 17, JPA/Hibernate, JUnit 5 + Mockito

---

## Mapa de Arquivos

| Arquivo | Ação | Responsabilidade |
|---|---|---|
| `enums/ScheduleItemType.java` | Criar | Enum MUSIC / TEXT |
| `models/EventScheduleItem.java` | Criar | Entidade JPA da programação |
| `repositories/EventScheduleItemRepository.java` | Criar | Queries de acesso à programação |
| `shared/dto/Schedule/ScheduleItemResponse.java` | Criar | DTO de resposta do GET |
| `shared/dto/Schedule/ScheduleMusicResponse.java` | Criar | DTO aninhado da música dentro do item |
| `shared/dto/Schedule/CreateTextScheduleItemRequest.java` | Criar | DTO de criação de item de texto |
| `shared/dto/Schedule/UpdateTextScheduleItemRequest.java` | Criar | DTO de edição de item de texto |
| `shared/dto/Schedule/ReorderScheduleRequest.java` | Criar | DTO de reordenação |
| `services/ScheduleService.java` | Criar | Interface do serviço |
| `services/impl/ScheduleServiceImpl.java` | Criar | Implementação do serviço |
| `controllers/EventScheduleController.java` | Criar | Controller REST `/events/{eventId}/schedule` |
| `services/impl/EventServiceImpl.java` | Modificar | Chamar hooks do ScheduleService |
| `tests/.../ScheduleServiceImplTest.java` | Criar | Testes unitários do serviço |

---

## Task 1: Enum e Entidade

**Files:**
- Create: `src/main/java/br/com/louvor4/api/enums/ScheduleItemType.java`
- Create: `src/main/java/br/com/louvor4/api/models/EventScheduleItem.java`

- [ ] **Step 1: Criar o enum ScheduleItemType**

```java
// src/main/java/br/com/louvor4/api/enums/ScheduleItemType.java
package br.com.louvor4.api.enums;

public enum ScheduleItemType {
    MUSIC,
    TEXT
}
```

- [ ] **Step 2: Criar a entidade EventScheduleItem**

```java
// src/main/java/br/com/louvor4/api/models/EventScheduleItem.java
package br.com.louvor4.api.models;

import br.com.louvor4.api.enums.ScheduleItemType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "event_schedule_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_event_schedule_setlist_item",
                        columnNames = {"event_id", "setlist_item_id"}
                )
        },
        indexes = {
                @Index(name = "idx_event_schedule_items_event_id", columnList = "event_id"),
                @Index(name = "idx_event_schedule_items_event_id_position", columnList = "event_id, position")
        }
)
public class EventScheduleItem {

    @Id
    @GeneratedValue
    @UuidGenerator
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, columnDefinition = "uuid")
    private Event event;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private ScheduleItemType type;

    @NotNull
    @Min(1)
    @Column(name = "position", nullable = false)
    private Integer position;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "setlist_item_id", columnDefinition = "uuid")
    private EventSetlistItem setlistItem;

    @Column(name = "title", length = 255)
    private String title;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        var now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isMusic() {
        return ScheduleItemType.MUSIC.equals(this.type);
    }

    public boolean isText() {
        return ScheduleItemType.TEXT.equals(this.type);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Event getEvent() { return event; }
    public void setEvent(Event event) { this.event = event; }
    public ScheduleItemType getType() { return type; }
    public void setType(ScheduleItemType type) { this.type = type; }
    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }
    public EventSetlistItem getSetlistItem() { return setlistItem; }
    public void setSetlistItem(EventSetlistItem setlistItem) { this.setlistItem = setlistItem; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/br/com/louvor4/api/enums/ScheduleItemType.java \
        src/main/java/br/com/louvor4/api/models/EventScheduleItem.java
git commit -m "feat: adiciona entidade EventScheduleItem e enum ScheduleItemType"
```

---

## Task 2: Repositório

**Files:**
- Create: `src/main/java/br/com/louvor4/api/repositories/EventScheduleItemRepository.java`

- [ ] **Step 1: Criar o repositório**

```java
// src/main/java/br/com/louvor4/api/repositories/EventScheduleItemRepository.java
package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.EventScheduleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EventScheduleItemRepository extends JpaRepository<EventScheduleItem, UUID> {

    List<EventScheduleItem> findByEventIdOrderByPositionAsc(UUID eventId);

    @Query("""
            select coalesce(max(s.position), 0)
            from EventScheduleItem s
            where s.event.id = :eventId
            """)
    Integer findMaxPositionByEventId(@Param("eventId") UUID eventId);

    void deleteBySetlistItemId(UUID setlistItemId);
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/br/com/louvor4/api/repositories/EventScheduleItemRepository.java
git commit -m "feat: adiciona EventScheduleItemRepository"
```

---

## Task 3: DTOs

**Files:**
- Create: `src/main/java/br/com/louvor4/api/shared/dto/Schedule/ScheduleMusicResponse.java`
- Create: `src/main/java/br/com/louvor4/api/shared/dto/Schedule/ScheduleItemResponse.java`
- Create: `src/main/java/br/com/louvor4/api/shared/dto/Schedule/CreateTextScheduleItemRequest.java`
- Create: `src/main/java/br/com/louvor4/api/shared/dto/Schedule/UpdateTextScheduleItemRequest.java`
- Create: `src/main/java/br/com/louvor4/api/shared/dto/Schedule/ReorderScheduleRequest.java`

- [ ] **Step 1: Criar ScheduleMusicResponse**

```java
// src/main/java/br/com/louvor4/api/shared/dto/Schedule/ScheduleMusicResponse.java
package br.com.louvor4.api.shared.dto.Schedule;

import java.util.UUID;

public record ScheduleMusicResponse(
        UUID id,
        String title,
        String artist
) {}
```

- [ ] **Step 2: Criar ScheduleItemResponse**

```java
// src/main/java/br/com/louvor4/api/shared/dto/Schedule/ScheduleItemResponse.java
package br.com.louvor4.api.shared.dto.Schedule;

import br.com.louvor4.api.enums.ScheduleItemType;

import java.util.UUID;

public record ScheduleItemResponse(
        UUID id,
        ScheduleItemType type,
        Integer position,
        String title,
        String description,
        ScheduleMusicResponse music
) {}
```

- [ ] **Step 3: Criar CreateTextScheduleItemRequest**

```java
// src/main/java/br/com/louvor4/api/shared/dto/Schedule/CreateTextScheduleItemRequest.java
package br.com.louvor4.api.shared.dto.Schedule;

import jakarta.validation.constraints.NotBlank;

public record CreateTextScheduleItemRequest(
        @NotBlank String title,
        String description
) {}
```

- [ ] **Step 4: Criar UpdateTextScheduleItemRequest**

```java
// src/main/java/br/com/louvor4/api/shared/dto/Schedule/UpdateTextScheduleItemRequest.java
package br.com.louvor4.api.shared.dto.Schedule;

import jakarta.validation.constraints.NotBlank;

public record UpdateTextScheduleItemRequest(
        @NotBlank String title,
        String description
) {}
```

- [ ] **Step 5: Criar ReorderScheduleRequest**

```java
// src/main/java/br/com/louvor4/api/shared/dto/Schedule/ReorderScheduleRequest.java
package br.com.louvor4.api.shared.dto.Schedule;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record ReorderScheduleRequest(
        @NotNull @NotEmpty List<UUID> orderedIds
) {}
```

- [ ] **Step 6: Commit**

```bash
git add src/main/java/br/com/louvor4/api/shared/dto/Schedule/
git commit -m "feat: adiciona DTOs do Schedule"
```

---

## Task 4: Interface de Serviço

**Files:**
- Create: `src/main/java/br/com/louvor4/api/services/ScheduleService.java`

- [ ] **Step 1: Criar a interface ScheduleService**

```java
// src/main/java/br/com/louvor4/api/services/ScheduleService.java
package br.com.louvor4.api.services;

import br.com.louvor4.api.models.EventSetlistItem;
import br.com.louvor4.api.shared.dto.Schedule.CreateTextScheduleItemRequest;
import br.com.louvor4.api.shared.dto.Schedule.ReorderScheduleRequest;
import br.com.louvor4.api.shared.dto.Schedule.ScheduleItemResponse;
import br.com.louvor4.api.shared.dto.Schedule.UpdateTextScheduleItemRequest;

import java.util.List;
import java.util.UUID;

public interface ScheduleService {

    List<ScheduleItemResponse> getSchedule(UUID eventId);

    void addTextItem(UUID eventId, CreateTextScheduleItemRequest request);

    void updateTextItem(UUID eventId, UUID itemId, UpdateTextScheduleItemRequest request);

    void deleteTextItem(UUID eventId, UUID itemId);

    void reorder(UUID eventId, ReorderScheduleRequest request);

    void onSetlistItemAdded(EventSetlistItem setlistItem);

    void onSetlistItemRemoved(UUID setlistItemId);
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/br/com/louvor4/api/services/ScheduleService.java
git commit -m "feat: adiciona interface ScheduleService"
```

---

## Task 5: Testes unitários (TDD)

**Files:**
- Create: `src/test/java/br/com/louvor4/api/services/impl/ScheduleServiceImplTest.java`

- [ ] **Step 1: Criar a classe de testes com casos failing**

```java
// src/test/java/br/com/louvor4/api/services/impl/ScheduleServiceImplTest.java
package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.ScheduleItemType;
import br.com.louvor4.api.enums.SetlistItemType;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.*;
import br.com.louvor4.api.repositories.EventRepository;
import br.com.louvor4.api.repositories.EventScheduleItemRepository;
import br.com.louvor4.api.shared.dto.Schedule.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceImplTest {

    @Mock
    private EventScheduleItemRepository scheduleItemRepository;

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private ScheduleServiceImpl scheduleService;

    // ---- getSchedule ----

    @Test
    void getScheduleShouldReturnItemsOrderedByPosition() {
        UUID eventId = UUID.randomUUID();
        Event event = new Event();
        event.setId(eventId);

        Song song = new Song();
        song.setId(UUID.randomUUID());
        song.setTitle("Superman");
        song.setArtist("Fruto Sagrado");

        EventSetlistItem setlistItem = new EventSetlistItem();
        setlistItem.setId(UUID.randomUUID());
        setlistItem.setSong(song);

        EventScheduleItem musicItem = new EventScheduleItem();
        musicItem.setId(UUID.randomUUID());
        musicItem.setEvent(event);
        musicItem.setType(ScheduleItemType.MUSIC);
        musicItem.setPosition(1000);
        musicItem.setSetlistItem(setlistItem);

        EventScheduleItem textItem = new EventScheduleItem();
        textItem.setId(UUID.randomUUID());
        textItem.setEvent(event);
        textItem.setType(ScheduleItemType.TEXT);
        textItem.setPosition(2000);
        textItem.setTitle("Oração");
        textItem.setDescription("Momento de oração coletiva");

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(scheduleItemRepository.findByEventIdOrderByPositionAsc(eventId))
                .thenReturn(List.of(musicItem, textItem));

        List<ScheduleItemResponse> result = scheduleService.getSchedule(eventId);

        assertEquals(2, result.size());

        ScheduleItemResponse first = result.get(0);
        assertEquals(ScheduleItemType.MUSIC, first.type());
        assertEquals(1000, first.position());
        assertNotNull(first.music());
        assertEquals("Superman", first.music().title());
        assertEquals("Fruto Sagrado", first.music().artist());
        assertNull(first.title());

        ScheduleItemResponse second = result.get(1);
        assertEquals(ScheduleItemType.TEXT, second.type());
        assertEquals(2000, second.position());
        assertEquals("Oração", second.title());
        assertEquals("Momento de oração coletiva", second.description());
        assertNull(second.music());
    }

    @Test
    void getScheduleShouldThrowWhenEventNotFound() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> scheduleService.getSchedule(eventId));
    }

    // ---- addTextItem ----

    @Test
    void addTextItemShouldPersistWithPositionAfterMax() {
        UUID eventId = UUID.randomUUID();
        Event event = new Event();
        event.setId(eventId);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(scheduleItemRepository.findMaxPositionByEventId(eventId)).thenReturn(2000);

        scheduleService.addTextItem(eventId, new CreateTextScheduleItemRequest("Oração", "Descrição"));

        ArgumentCaptor<EventScheduleItem> captor = ArgumentCaptor.forClass(EventScheduleItem.class);
        verify(scheduleItemRepository).save(captor.capture());

        EventScheduleItem saved = captor.getValue();
        assertEquals(ScheduleItemType.TEXT, saved.getType());
        assertEquals(3000, saved.getPosition());
        assertEquals("Oração", saved.getTitle());
        assertEquals("Descrição", saved.getDescription());
    }

    @Test
    void addTextItemShouldUse1000AsFirstPositionWhenNoItems() {
        UUID eventId = UUID.randomUUID();
        Event event = new Event();
        event.setId(eventId);

        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
        when(scheduleItemRepository.findMaxPositionByEventId(eventId)).thenReturn(0);

        scheduleService.addTextItem(eventId, new CreateTextScheduleItemRequest("Início", null));

        ArgumentCaptor<EventScheduleItem> captor = ArgumentCaptor.forClass(EventScheduleItem.class);
        verify(scheduleItemRepository).save(captor.capture());
        assertEquals(1000, captor.getValue().getPosition());
    }

    // ---- updateTextItem ----

    @Test
    void updateTextItemShouldChangeTitleAndDescription() {
        UUID eventId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);

        EventScheduleItem item = new EventScheduleItem();
        item.setId(itemId);
        item.setEvent(event);
        item.setType(ScheduleItemType.TEXT);
        item.setTitle("Antigo");

        when(scheduleItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        scheduleService.updateTextItem(eventId, itemId, new UpdateTextScheduleItemRequest("Novo", "Nova desc"));

        verify(scheduleItemRepository).save(item);
        assertEquals("Novo", item.getTitle());
        assertEquals("Nova desc", item.getDescription());
    }

    @Test
    void updateTextItemShouldThrowWhenItemNotFound() {
        UUID eventId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        when(scheduleItemRepository.findById(itemId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class,
                () -> scheduleService.updateTextItem(eventId, itemId, new UpdateTextScheduleItemRequest("X", null)));
    }

    @Test
    void updateTextItemShouldThrowWhenItemBelongsToDifferentEvent() {
        UUID eventId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        Event otherEvent = new Event();
        otherEvent.setId(UUID.randomUUID());

        EventScheduleItem item = new EventScheduleItem();
        item.setId(itemId);
        item.setEvent(otherEvent);
        item.setType(ScheduleItemType.TEXT);

        when(scheduleItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        assertThrows(ValidationException.class,
                () -> scheduleService.updateTextItem(eventId, itemId, new UpdateTextScheduleItemRequest("X", null)));
    }

    @Test
    void updateTextItemShouldThrowWhenItemIsNotText() {
        UUID eventId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);

        EventScheduleItem item = new EventScheduleItem();
        item.setId(itemId);
        item.setEvent(event);
        item.setType(ScheduleItemType.MUSIC);

        when(scheduleItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        assertThrows(ValidationException.class,
                () -> scheduleService.updateTextItem(eventId, itemId, new UpdateTextScheduleItemRequest("X", null)));
    }

    // ---- deleteTextItem ----

    @Test
    void deleteTextItemShouldRemoveTextItem() {
        UUID eventId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);

        EventScheduleItem item = new EventScheduleItem();
        item.setId(itemId);
        item.setEvent(event);
        item.setType(ScheduleItemType.TEXT);

        when(scheduleItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        scheduleService.deleteTextItem(eventId, itemId);

        verify(scheduleItemRepository).delete(item);
    }

    @Test
    void deleteTextItemShouldThrowWhenItemIsMusic() {
        UUID eventId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);

        EventScheduleItem item = new EventScheduleItem();
        item.setId(itemId);
        item.setEvent(event);
        item.setType(ScheduleItemType.MUSIC);

        when(scheduleItemRepository.findById(itemId)).thenReturn(Optional.of(item));

        assertThrows(ValidationException.class, () -> scheduleService.deleteTextItem(eventId, itemId));
    }

    // ---- reorder ----

    @Test
    void reorderShouldReassignPositionsInMultiplesOf1000() {
        UUID eventId = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        UUID id3 = UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);

        EventScheduleItem item1 = new EventScheduleItem();
        item1.setId(id1);
        item1.setEvent(event);
        item1.setPosition(3000);

        EventScheduleItem item2 = new EventScheduleItem();
        item2.setId(id2);
        item2.setEvent(event);
        item2.setPosition(1000);

        EventScheduleItem item3 = new EventScheduleItem();
        item3.setId(id3);
        item3.setEvent(event);
        item3.setPosition(2000);

        when(scheduleItemRepository.findByEventIdOrderByPositionAsc(eventId))
                .thenReturn(List.of(item2, item3, item1));

        scheduleService.reorder(eventId, new ReorderScheduleRequest(List.of(id1, id2, id3)));

        verify(scheduleItemRepository).saveAll(any());
        assertEquals(1000, item1.getPosition());
        assertEquals(2000, item2.getPosition());
        assertEquals(3000, item3.getPosition());
    }

    @Test
    void reorderShouldThrowWhenOrderedIdsCountDiffersFromScheduleCount() {
        UUID eventId = UUID.randomUUID();
        UUID id1 = UUID.randomUUID();

        Event event = new Event();
        event.setId(eventId);

        EventScheduleItem item = new EventScheduleItem();
        item.setId(id1);
        item.setEvent(event);
        item.setPosition(1000);

        EventScheduleItem extra = new EventScheduleItem();
        extra.setId(UUID.randomUUID());
        extra.setEvent(event);
        extra.setPosition(2000);

        when(scheduleItemRepository.findByEventIdOrderByPositionAsc(eventId))
                .thenReturn(List.of(item, extra));

        assertThrows(ValidationException.class,
                () -> scheduleService.reorder(eventId, new ReorderScheduleRequest(List.of(id1))));
    }

    // ---- onSetlistItemAdded ----

    @Test
    void onSetlistItemAddedShouldCreateMusicItemAtEnd() {
        Event event = new Event();
        event.setId(UUID.randomUUID());

        EventSetlistItem setlistItem = new EventSetlistItem();
        setlistItem.setId(UUID.randomUUID());
        setlistItem.setEvent(event);

        when(scheduleItemRepository.findMaxPositionByEventId(event.getId())).thenReturn(1000);

        scheduleService.onSetlistItemAdded(setlistItem);

        ArgumentCaptor<EventScheduleItem> captor = ArgumentCaptor.forClass(EventScheduleItem.class);
        verify(scheduleItemRepository).save(captor.capture());

        EventScheduleItem saved = captor.getValue();
        assertEquals(ScheduleItemType.MUSIC, saved.getType());
        assertEquals(2000, saved.getPosition());
        assertEquals(setlistItem, saved.getSetlistItem());
        assertEquals(event, saved.getEvent());
    }

    // ---- onSetlistItemRemoved ----

    @Test
    void onSetlistItemRemovedShouldDeleteCorrespondingScheduleItem() {
        UUID setlistItemId = UUID.randomUUID();

        scheduleService.onSetlistItemRemoved(setlistItemId);

        verify(scheduleItemRepository).deleteBySetlistItemId(setlistItemId);
    }
}
```

- [ ] **Step 2: Rodar os testes — confirmar que falham por classe não existir**

```bash
./mvnw test -pl . -Dtest=ScheduleServiceImplTest -q 2>&1 | tail -20
```
Esperado: compilation error ou `ClassNotFoundException` para `ScheduleServiceImpl`.

---

## Task 6: Implementação do Serviço

**Files:**
- Create: `src/main/java/br/com/louvor4/api/services/impl/ScheduleServiceImpl.java`

- [ ] **Step 1: Criar ScheduleServiceImpl**

```java
// src/main/java/br/com/louvor4/api/services/impl/ScheduleServiceImpl.java
package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.ScheduleItemType;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.EventScheduleItem;
import br.com.louvor4.api.models.EventSetlistItem;
import br.com.louvor4.api.repositories.EventRepository;
import br.com.louvor4.api.repositories.EventScheduleItemRepository;
import br.com.louvor4.api.services.ScheduleService;
import br.com.louvor4.api.shared.dto.Schedule.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleServiceImpl implements ScheduleService {

    private static final int POSITION_GAP = 1000;

    private final EventScheduleItemRepository scheduleItemRepository;
    private final EventRepository eventRepository;

    public ScheduleServiceImpl(EventScheduleItemRepository scheduleItemRepository,
                               EventRepository eventRepository) {
        this.scheduleItemRepository = scheduleItemRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public List<ScheduleItemResponse> getSchedule(UUID eventId) {
        eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Evento não encontrado."));

        return scheduleItemRepository.findByEventIdOrderByPositionAsc(eventId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void addTextItem(UUID eventId, CreateTextScheduleItemRequest request) {
        var event = eventRepository.findById(eventId)
                .orElseThrow(() -> new NotFoundException("Evento não encontrado."));

        int nextPosition = nextPosition(eventId);

        var item = new EventScheduleItem();
        item.setEvent(event);
        item.setType(ScheduleItemType.TEXT);
        item.setPosition(nextPosition);
        item.setTitle(request.title());
        item.setDescription(request.description());

        scheduleItemRepository.save(item);
    }

    @Override
    @Transactional
    public void updateTextItem(UUID eventId, UUID itemId, UpdateTextScheduleItemRequest request) {
        var item = scheduleItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item da programação não encontrado."));

        if (!item.getEvent().getId().equals(eventId)) {
            throw new ValidationException("Item não pertence ao evento informado.");
        }
        if (!item.isText()) {
            throw new ValidationException("Apenas itens do tipo TEXT podem ser editados.");
        }

        item.setTitle(request.title());
        item.setDescription(request.description());
        scheduleItemRepository.save(item);
    }

    @Override
    @Transactional
    public void deleteTextItem(UUID eventId, UUID itemId) {
        var item = scheduleItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Item da programação não encontrado."));

        if (!item.getEvent().getId().equals(eventId)) {
            throw new ValidationException("Item não pertence ao evento informado.");
        }
        if (!item.isText()) {
            throw new ValidationException("Apenas itens do tipo TEXT podem ser removidos diretamente.");
        }

        scheduleItemRepository.delete(item);
    }

    @Override
    @Transactional
    public void reorder(UUID eventId, ReorderScheduleRequest request) {
        List<EventScheduleItem> currentItems =
                scheduleItemRepository.findByEventIdOrderByPositionAsc(eventId);

        List<UUID> orderedIds = request.orderedIds();

        if (currentItems.size() != orderedIds.size()) {
            throw new ValidationException(
                    "A lista de IDs deve conter exatamente " + currentItems.size() + " itens.");
        }

        Map<UUID, EventScheduleItem> itemById = currentItems.stream()
                .collect(Collectors.toMap(EventScheduleItem::getId, i -> i));

        List<EventScheduleItem> toSave = new ArrayList<>(orderedIds.size());
        int position = POSITION_GAP;
        for (UUID id : orderedIds) {
            EventScheduleItem item = itemById.get(id);
            if (item == null) {
                throw new ValidationException("Item não encontrado na programação do evento: " + id);
            }
            item.setPosition(position);
            toSave.add(item);
            position += POSITION_GAP;
        }

        scheduleItemRepository.saveAll(toSave);
    }

    @Override
    @Transactional
    public void onSetlistItemAdded(EventSetlistItem setlistItem) {
        int nextPosition = nextPosition(setlistItem.getEvent().getId());

        var item = new EventScheduleItem();
        item.setEvent(setlistItem.getEvent());
        item.setType(ScheduleItemType.MUSIC);
        item.setPosition(nextPosition);
        item.setSetlistItem(setlistItem);

        scheduleItemRepository.save(item);
    }

    @Override
    @Transactional
    public void onSetlistItemRemoved(UUID setlistItemId) {
        scheduleItemRepository.deleteBySetlistItemId(setlistItemId);
    }

    private int nextPosition(UUID eventId) {
        int max = scheduleItemRepository.findMaxPositionByEventId(eventId);
        return max + POSITION_GAP;
    }

    private ScheduleItemResponse toResponse(EventScheduleItem item) {
        ScheduleMusicResponse music = null;
        if (item.isMusic() && item.getSetlistItem() != null) {
            var song = item.getSetlistItem().getSong();
            music = new ScheduleMusicResponse(
                    song != null ? song.getId() : null,
                    song != null ? song.getTitle() : null,
                    song != null ? song.getArtist() : null
            );
        }
        return new ScheduleItemResponse(
                item.getId(),
                item.getType(),
                item.getPosition(),
                item.isText() ? item.getTitle() : null,
                item.isText() ? item.getDescription() : null,
                music
        );
    }
}
```

- [ ] **Step 2: Rodar os testes — confirmar que passam**

```bash
./mvnw test -pl . -Dtest=ScheduleServiceImplTest -q 2>&1 | tail -20
```
Esperado: `BUILD SUCCESS`, todos os testes passam.

- [ ] **Step 3: Commit**

```bash
git add src/main/java/br/com/louvor4/api/services/impl/ScheduleServiceImpl.java \
        src/test/java/br/com/louvor4/api/services/impl/ScheduleServiceImplTest.java
git commit -m "feat: implementa ScheduleServiceImpl com testes"
```

---

## Task 7: Controller

**Files:**
- Create: `src/main/java/br/com/louvor4/api/controllers/EventScheduleController.java`

- [ ] **Step 1: Criar EventScheduleController**

```java
// src/main/java/br/com/louvor4/api/controllers/EventScheduleController.java
package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.ScheduleService;
import br.com.louvor4.api.shared.dto.Schedule.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events/{eventId}/schedule")
public class EventScheduleController {

    private final ScheduleService scheduleService;

    public EventScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public ResponseEntity<List<ScheduleItemResponse>> getSchedule(@PathVariable UUID eventId) {
        return ResponseEntity.ok(scheduleService.getSchedule(eventId));
    }

    @PostMapping("/text")
    public ResponseEntity<Void> addTextItem(
            @PathVariable UUID eventId,
            @RequestBody @Valid CreateTextScheduleItemRequest request) {
        scheduleService.addTextItem(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<Void> updateTextItem(
            @PathVariable UUID eventId,
            @PathVariable UUID itemId,
            @RequestBody @Valid UpdateTextScheduleItemRequest request) {
        scheduleService.updateTextItem(eventId, itemId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> deleteTextItem(
            @PathVariable UUID eventId,
            @PathVariable UUID itemId) {
        scheduleService.deleteTextItem(eventId, itemId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/reorder")
    public ResponseEntity<Void> reorder(
            @PathVariable UUID eventId,
            @RequestBody @Valid ReorderScheduleRequest request) {
        scheduleService.reorder(eventId, request);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/br/com/louvor4/api/controllers/EventScheduleController.java
git commit -m "feat: adiciona EventScheduleController"
```

---

## Task 8: Integração com o repertório (EventServiceImpl)

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/services/impl/EventServiceImpl.java`

O objetivo é chamar `scheduleService.onSetlistItemAdded()` e `scheduleService.onSetlistItemRemoved()` nos métodos existentes de add/remove do setlist.

- [ ] **Step 1: Injetar ScheduleService no EventServiceImpl**

Localizar o construtor de `EventServiceImpl` e adicionar a dependência:

```java
// No topo da classe, adicionar o campo:
private final ScheduleService scheduleService;

// No construtor, adicionar o parâmetro e a atribuição:
public EventServiceImpl(
        EventRepository eventRepository,
        EventParticipantRepository eventParticipantRepository,
        MusicProjectMemberRepository musicProjectMemberRepository,
        EventMapper eventMapper,
        EventSetlistItemMapper eventSetlistItemMapper,
        CurrentUserProvider currentUserProvider,
        ProjectSkillRepository projectSkillRepository,
        SongRepository songRepository,
        EventSetlistItemRepository eventSetlistItemRepository,
        PushSenderService senderService,
        UserNotificationService userNotificationService,
        UserUnavailabilityRepository userUnavailabilityRepository,
        EventSetlistItemStrategyResolver strategyResolver,
        ScheduleService scheduleService          // <-- novo
) {
    // ... atribuições existentes ...
    this.scheduleService = scheduleService;      // <-- novo
}
```

- [ ] **Step 2: Chamar onSetlistItemAdded em addSetListItemToEvent**

No método `addSetListItemToEvent`, após `eventSetlistItemRepository.saveAll(toSave)`, adicionar:

```java
for (EventSetlistItem saved : toSave) {
    scheduleService.onSetlistItemAdded(saved);
}
```

- [ ] **Step 3: Chamar onSetlistItemRemoved em removeSetlistItemFromEvent**

No método `removeSetlistItemFromEvent`, **antes** de `eventSetlistItemRepository.delete(setlistItem)`, adicionar (a remoção do schedule item deve preceder a do setlist item para evitar violação de FK):

```java
scheduleService.onSetlistItemRemoved(setlistItem.getId());
// eventSetlistItemRepository.delete(setlistItem); ← linha que já existe, não duplicar
```

- [ ] **Step 4: Adicionar o import**

```java
import br.com.louvor4.api.services.ScheduleService;
```

- [ ] **Step 5: Rodar todos os testes para confirmar nenhuma regressão**

```bash
./mvnw test -q 2>&1 | tail -30
```
Esperado: `BUILD SUCCESS`.

- [ ] **Step 6: Commit**

```bash
git add src/main/java/br/com/louvor4/api/services/impl/EventServiceImpl.java
git commit -m "feat: conecta EventServiceImpl ao ScheduleService para sincronizar programação"
```

---

## Checklist de Cobertura da Spec

| Requisito | Task |
|---|---|
| GET /schedule retorna itens ordenados por position | Task 6 (getSchedule) + Task 7 |
| POST /schedule/text cria item de texto no final | Task 6 (addTextItem) + Task 7 |
| PUT /schedule/{itemId} edita item de texto | Task 6 (updateTextItem) + Task 7 |
| DELETE /schedule/{itemId} remove item de texto | Task 6 (deleteTextItem) + Task 7 |
| PATCH /schedule/reorder reordena por lista de IDs | Task 6 (reorder) + Task 7 |
| Adição ao repertório → inserção automática no schedule | Task 8 |
| Remoção do repertório → remoção automática no schedule | Task 8 |
| Músicas não podem ser adicionadas/removidas via schedule | Task 6 (guarda validação) |
| Posição com gap de 1000 | Task 6 (nextPosition + reorder) |
| Resposta MUSIC tem objeto `music` com id/title/artist | Task 6 (toResponse) |
| Resposta TEXT tem title e description | Task 6 (toResponse) |
