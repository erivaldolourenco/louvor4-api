# Past Events with Infinite Scroll — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a "Passados" tab on the home screen that lists the user's past events with infinite scroll, backed by a new paginated `GET /users/events/past` endpoint.

**Architecture:** The backend adds a `Page<EventParticipant>` JPQL query with a separate count query (required when using `join fetch` with Spring Data JPA pagination), a new `getPastEventsByUser(Pageable)` method in `EventServiceImpl` that mirrors the existing `getEventsByUser` logic, and a `@GetMapping("/events/past")` endpoint in `UserController`. The frontend extends `HomeStore` with paginated past-events state, wires an `IntersectionObserver` to a sentinel `<div>` at the bottom of the list, and adds tab switching to `HomeComponent`.

**Tech Stack:** Spring Boot 3 / Spring Data JPA / Pageable (backend), Angular 17+ standalone components with signals and `IntersectionObserver` (frontend)

---

## File Map

| File | Action |
|------|--------|
| `src/main/java/br/com/louvor4/api/repositories/EventParticipantRepository.java` | Modify — add paginated past-events query |
| `src/test/java/br/com/louvor4/api/services/impl/EventServiceImplGetPastEventsTest.java` | Create — unit tests for `getPastEventsByUser` |
| `src/main/java/br/com/louvor4/api/services/EventService.java` | Modify — add `getPastEventsByUser` method |
| `src/main/java/br/com/louvor4/api/services/impl/EventServiceImpl.java` | Modify — implement `getPastEventsByUser` |
| `src/main/java/br/com/louvor4/api/controllers/UserController.java` | Modify — add `GET /users/events/past` endpoint |
| `src/app/core/models/page.model.ts` (louvor4-web) | Create — `Page<T>` interface |
| `src/app/core/services/user.service.ts` (louvor4-web) | Modify — add `getPastEvents` |
| `src/app/pages/home/home.store.ts` (louvor4-web) | Modify — add past events state + load methods |
| `src/app/pages/home/home.component.ts` (louvor4-web) | Modify — tabs + `IntersectionObserver` |
| `src/app/pages/home/home.component.html` (louvor4-web) | Modify — tab bar + past events section |

---

### Task 1: Add paginated past-events JPQL query

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/repositories/EventParticipantRepository.java`

The Spring Data JPA count query for a paginated result CANNOT use `join fetch` — Hibernate strips the fetch but not reliably. We must supply a `countQuery` separately.

- [ ] **Step 1: Add the paginated query method**

Add to `EventParticipantRepository` (after the existing `findAcceptedByUserWithEventAndProjectAndMemberUser` query). Also add `import org.springframework.data.domain.Page;` and `import org.springframework.data.domain.Pageable;` at the top.

```java
@Query(
    value = """
            select ep from EventParticipant ep
            join fetch ep.event e
            join fetch e.musicProject
            join fetch ep.member m
            join fetch m.user u
            where u.id = :userId
              and ep.status = :status
              and e.startAt < :now
            order by e.startAt desc
            """,
    countQuery = """
            select count(ep) from EventParticipant ep
            join ep.event e
            join ep.member m
            join m.user u
            where u.id = :userId
              and ep.status = :status
              and e.startAt < :now
            """
)
Page<EventParticipant> findPastByUserWithEventAndProjectAndMemberUser(
        @Param("userId") UUID userId,
        @Param("status") EventParticipantStatus status,
        @Param("now") LocalDateTime now,
        Pageable pageable);
```

- [ ] **Step 2: Compile to verify no errors**

```bash
cd /home/erivaldo/repositorio/erivaldo/louvor4-api
./mvnw -s ~/.m2/settings.xml compile -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

- [ ] **Step 3: Commit**

```bash
git add src/main/java/br/com/louvor4/api/repositories/EventParticipantRepository.java
git commit -m "feat: adiciona query paginada de eventos passados em EventParticipantRepository"
```

---

### Task 2: Write failing unit tests for getPastEventsByUser

**Files:**
- Create: `src/test/java/br/com/louvor4/api/services/impl/EventServiceImplGetPastEventsTest.java`

- [ ] **Step 1: Create the test file**

```java
package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.EventParticipantStatus;
import br.com.louvor4.api.enums.SetlistItemType;
import br.com.louvor4.api.mapper.EventMapper;
import br.com.louvor4.api.mapper.EventSetlistItemMapper;
import br.com.louvor4.api.models.*;
import br.com.louvor4.api.repositories.*;
import br.com.louvor4.api.services.EventReminderScheduler;
import br.com.louvor4.api.services.PushSenderService;
import br.com.louvor4.api.services.ProgramService;
import br.com.louvor4.api.services.UserNotificationService;
import br.com.louvor4.api.shared.dto.Event.UserEventDetailDto;
import br.com.louvor4.api.strategy.event.EventSetlistItemStrategyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplGetPastEventsTest {

    @Mock EventRepository eventRepository;
    @Mock EventParticipantRepository eventParticipantRepository;
    @Mock MusicProjectMemberRepository musicProjectMemberRepository;
    @Mock EventMapper eventMapper;
    @Mock EventSetlistItemMapper eventSetlistItemMapper;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock ProjectSkillRepository projectSkillRepository;
    @Mock SongRepository songRepository;
    @Mock EventSetlistItemRepository eventSetlistItemRepository;
    @Mock PushSenderService senderService;
    @Mock UserNotificationService userNotificationService;
    @Mock UserUnavailabilityRepository userUnavailabilityRepository;
    @Mock EventSetlistItemStrategyResolver strategyResolver;
    @Mock ProgramService programService;
    @Mock EventReminderScheduler eventReminderScheduler;
    @InjectMocks EventServiceImpl service;

    private UUID userId;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "startAt"));
        User user = new User();
        user.setId(userId);
        when(currentUserProvider.get()).thenReturn(user);
    }

    @Test
    void getPastEventsByUser_returnsEmptyPageWhenNoParticipants() {
        when(eventParticipantRepository.findPastByUserWithEventAndProjectAndMemberUser(
                eq(userId), eq(EventParticipantStatus.ACCEPTED), any(LocalDateTime.class), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        Page<UserEventDetailDto> result = service.getPastEventsByUser(pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getPastEventsByUser_returnsDtosForEachEvent() {
        MusicProject project = new MusicProject();
        project.setId(UUID.randomUUID());
        project.setName("Louvor");

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setTitle("Culto Passado");
        event.setStartAt(LocalDateTime.now().minusDays(7));
        event.setMusicProject(project);

        EventParticipant participant = new EventParticipant();
        participant.setEvent(event);
        participant.setMember(new MusicProjectMember());
        participant.setStatus(EventParticipantStatus.ACCEPTED);

        Page<EventParticipant> repoPage = new PageImpl<>(List.of(participant), pageable, 1);
        when(eventParticipantRepository.findPastByUserWithEventAndProjectAndMemberUser(
                eq(userId), eq(EventParticipantStatus.ACCEPTED), any(LocalDateTime.class), eq(pageable)))
                .thenReturn(repoPage);
        when(eventParticipantRepository.countDistinctMembersByEventIds(any())).thenReturn(List.of());
        when(eventParticipantRepository.findProfileImagesByEventIds(any())).thenReturn(List.of());
        when(eventSetlistItemRepository.countDistinctSongsByEventIds(any(), eq(SetlistItemType.SONG)))
                .thenReturn(List.of());

        Page<UserEventDetailDto> result = service.getPastEventsByUser(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Culto Passado");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getPastEventsByUser_passesAcceptedStatusAndCurrentTimeToQuery() {
        when(eventParticipantRepository.findPastByUserWithEventAndProjectAndMemberUser(
                eq(userId), eq(EventParticipantStatus.ACCEPTED), any(LocalDateTime.class), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        service.getPastEventsByUser(pageable);

        verify(eventParticipantRepository).findPastByUserWithEventAndProjectAndMemberUser(
                eq(userId),
                eq(EventParticipantStatus.ACCEPTED),
                any(LocalDateTime.class),
                eq(pageable));
    }
}
```

- [ ] **Step 2: Run the test — expect FAIL (method not yet implemented)**

```bash
cd /home/erivaldo/repositorio/erivaldo/louvor4-api
./mvnw -s ~/.m2/settings.xml test -pl . -Dtest=EventServiceImplGetPastEventsTest -q 2>&1 | tail -20
```

Expected: compilation error — `getPastEventsByUser` does not exist in `EventServiceImpl`.

---

### Task 3: Implement getPastEventsByUser

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/services/EventService.java`
- Modify: `src/main/java/br/com/louvor4/api/services/impl/EventServiceImpl.java`

- [ ] **Step 1: Add method to EventService interface**

Add the following imports to `EventService.java`:
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
```

Add after `getEventsByUser()`:
```java
Page<UserEventDetailDto> getPastEventsByUser(Pageable pageable);
```

- [ ] **Step 2: Add imports to EventServiceImpl**

Add to `EventServiceImpl.java` imports:
```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
```

- [ ] **Step 3: Add implementation method after getEventsByUser() (around line 429)**

```java
@Override
public Page<UserEventDetailDto> getPastEventsByUser(Pageable pageable) {
    UUID userId = currentUserProvider.get().getId();

    Page<EventParticipant> page = eventParticipantRepository
            .findPastByUserWithEventAndProjectAndMemberUser(
                    userId,
                    EventParticipantStatus.ACCEPTED,
                    LocalDateTime.now(),
                    pageable
            );

    List<EventParticipant> participants = page.getContent();

    if (participants.isEmpty()) {
        return new PageImpl<>(List.of(), pageable, 0);
    }

    List<Event> events = participants.stream()
            .map(EventParticipant::getEvent)
            .filter(Objects::nonNull)
            .distinct()
            .toList();

    List<UUID> eventIds = events.stream().map(Event::getId).toList();

    Map<UUID, Integer> participantCountByEvent = eventParticipantRepository
            .countDistinctMembersByEventIds(eventIds)
            .stream()
            .collect(Collectors.toMap(
                    EventCountProjection::getEventId,
                    count -> count.getTotal().intValue()
            ));

    Map<UUID, Integer> songCountByEvent = eventSetlistItemRepository
            .countDistinctSongsByEventIds(eventIds, SetlistItemType.SONG)
            .stream()
            .collect(Collectors.toMap(
                    EventCountProjection::getEventId,
                    count -> count.getTotal().intValue()
            ));

    Map<UUID, EventParticipant> participantByEventId = participants.stream()
            .filter(p -> p.getEvent() != null)
            .collect(Collectors.toMap(
                    p -> p.getEvent().getId(),
                    p -> p,
                    (left, right) -> left
            ));

    Map<UUID, List<String>> participantsImagesByEvent = buildParticipantsImagesByEvent(eventIds);

    List<UserEventDetailDto> dtos = events.stream()
            .map(event -> {
                EventParticipant ep = participantByEventId.get(event.getId());
                return new UserEventDetailDto(
                        event.getId(),
                        event.getMusicProject().getId(),
                        event.getTitle(),
                        event.getDescription(),
                        event.getStartAt().toLocalDate(),
                        event.getStartAt().toLocalTime(),
                        event.getLocation(),
                        event.getMusicProject().getName(),
                        event.getMusicProject().getProfileImage(),
                        participantCountByEvent.getOrDefault(event.getId(), 0),
                        songCountByEvent.getOrDefault(event.getId(), 0),
                        participantsImagesByEvent.getOrDefault(event.getId(), List.of()),
                        ep != null ? ep.getId() : null,
                        ep != null ? ep.getStatus() : null
                );
            })
            .toList();

    return new PageImpl<>(dtos, pageable, page.getTotalElements());
}
```

- [ ] **Step 4: Run unit tests — expect PASS**

```bash
cd /home/erivaldo/repositorio/erivaldo/louvor4-api
./mvnw -s ~/.m2/settings.xml test -pl . -Dtest=EventServiceImplGetPastEventsTest -q 2>&1 | tail -20
```

Expected: `BUILD SUCCESS`, 3 tests passing.

- [ ] **Step 5: Run all tests to confirm no regressions**

```bash
./mvnw -s ~/.m2/settings.xml test -pl . -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

- [ ] **Step 6: Commit**

```bash
git add src/main/java/br/com/louvor4/api/services/EventService.java \
        src/main/java/br/com/louvor4/api/services/impl/EventServiceImpl.java \
        src/test/java/br/com/louvor4/api/services/impl/EventServiceImplGetPastEventsTest.java
git commit -m "feat: implementa getPastEventsByUser paginado em EventServiceImpl"
```

---

### Task 4: Add GET /users/events/past endpoint

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/controllers/UserController.java`

- [ ] **Step 1: Add imports to UserController**

```java
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
```

- [ ] **Step 2: Add endpoint after getEventsByUser()**

```java
@GetMapping("/events/past")
public ResponseEntity<Page<UserEventDetailDto>> getPastEventsByUser(
        @PageableDefault(size = 10, sort = "startAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(eventService.getPastEventsByUser(pageable));
}
```

- [ ] **Step 3: Compile to verify**

```bash
cd /home/erivaldo/repositorio/erivaldo/louvor4-api
./mvnw -s ~/.m2/settings.xml compile -q 2>&1 | tail -10
```

Expected: `BUILD SUCCESS`

- [ ] **Step 4: Commit**

```bash
git add src/main/java/br/com/louvor4/api/controllers/UserController.java
git commit -m "feat: adiciona endpoint GET /users/events/past paginado"
```

---

### Task 5: Frontend — Page<T> model and UserService method

Frontend is in `/home/erivaldo/repositorio/erivaldo/louvor4-web`.

**Files:**
- Create: `src/app/core/models/page.model.ts`
- Modify: `src/app/core/services/user.service.ts`

- [ ] **Step 1: Create Page<T> model**

```ts
// src/app/core/models/page.model.ts
export interface Page<T> {
  content: T[];
  last: boolean;
  totalElements: number;
}
```

- [ ] **Step 2: Add import and getPastEvents to UserService**

Add import at top of `user.service.ts`:
```ts
import { Page } from '../models/page.model';
```

Add after `getEvents()`:
```ts
getPastEvents(page: number, size = 10): Observable<Page<MusicEventDetail>> {
  return this.http.get<Page<MusicEventDetail>>(
    `${this.baseUrl}/events/past?page=${page}&size=${size}`
  );
}
```

- [ ] **Step 3: Build to verify**

```bash
cd /home/erivaldo/repositorio/erivaldo/louvor4-web
ng build 2>&1 | tail -10
```

Expected: `Application bundle generation complete.`

- [ ] **Step 4: Commit**

```bash
git add src/app/core/models/page.model.ts src/app/core/services/user.service.ts
git commit -m "feat: adiciona Page<T> model e getPastEvents em UserService"
```

---

### Task 6: HomeStore — past events state

**Files:**
- Modify: `src/app/pages/home/home.store.ts`

- [ ] **Step 1: Replace home.store.ts with the extended version**

```ts
import { Injectable, signal } from '@angular/core';
import { UserService } from '../../core/services/user.service';
import { MusicEventDetail } from '../../core/models/requests/music-event-detail.model';

@Injectable({ providedIn: 'root' })
export class HomeStore {
    // Upcoming (unchanged)
    events       = signal<MusicEventDetail[]>([]);
    isLoading    = signal<boolean>(true);

    // Past events
    pastEvents    = signal<MusicEventDetail[]>([]);
    pastPage      = signal(0);
    pastHasMore   = signal(true);
    isLoadingMore = signal(false);
    isPastLoading = signal(false);

    constructor(private userService: UserService) { }

    load() {
        if (this.events().length > 0) {
            this.isLoading.set(false);
            return;
        }
        this.isLoading.set(true);
        this.userService.getEvents().subscribe({
            next: list => {
                this.events.set(list ?? []);
                this.isLoading.set(false);
            },
            error: () => {
                this.events.set([]);
                this.isLoading.set(false);
            },
        });
    }

    loadPastEvents(): void {
        this.pastPage.set(0);
        this.pastHasMore.set(true);
        this.pastEvents.set([]);
        this.isPastLoading.set(true);
        this.userService.getPastEvents(0).subscribe({
            next: page => {
                this.pastEvents.set(page.content ?? []);
                this.pastHasMore.set(!page.last);
                this.isPastLoading.set(false);
            },
            error: () => {
                this.isPastLoading.set(false);
            },
        });
    }

    loadMorePastEvents(): void {
        if (!this.pastHasMore() || this.isLoadingMore()) return;
        this.isLoadingMore.set(true);
        const nextPage = this.pastPage() + 1;
        this.userService.getPastEvents(nextPage).subscribe({
            next: page => {
                this.pastPage.set(nextPage);
                this.pastEvents.update(current => [...current, ...(page.content ?? [])]);
                this.pastHasMore.set(!page.last);
                this.isLoadingMore.set(false);
            },
            error: () => {
                this.isLoadingMore.set(false);
            },
        });
    }
}
```

- [ ] **Step 2: Build to verify**

```bash
cd /home/erivaldo/repositorio/erivaldo/louvor4-web
ng build 2>&1 | tail -10
```

Expected: `Application bundle generation complete.`

- [ ] **Step 3: Commit**

```bash
git add src/app/pages/home/home.store.ts
git commit -m "feat: adiciona estado e métodos de eventos passados no HomeStore"
```

---

### Task 7: HomeComponent TS — tabs and IntersectionObserver

**Files:**
- Modify: `src/app/pages/home/home.component.ts`

- [ ] **Step 1: Replace home.component.ts**

```ts
import {
  AfterViewInit,
  Component,
  ElementRef,
  OnDestroy,
  OnInit,
  ViewChild,
  inject,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';
import { HomeStore } from './home.store';
import { MusicEventListItemComponent } from '../../shared/components/music-event-list-item/music-event-list-item.component';
import { PushNotificationService } from '../../core/services/push-notification.service';

@Component({
  selector: 'app-home',
  templateUrl: './home.component.html',
  styleUrl: './home.component.css',
  imports: [MusicEventListItemComponent],
  standalone: true,
})
export class HomeComponent implements OnInit, AfterViewInit, OnDestroy {
  private router = inject(Router);
  store = inject(HomeStore);
  private pushService = inject(PushNotificationService);

  pushEnabled  = signal(false);
  musicEvents  = this.store.events;
  isLoading    = this.store.isLoading;
  activeTab    = signal<'upcoming' | 'past'>('upcoming');

  @ViewChild('sentinel') sentinel?: ElementRef<HTMLElement>;
  private observer?: IntersectionObserver;

  ngOnInit(): void {
    this.store.load();
    if (localStorage.getItem('louvor4_push_enabled') === 'true') {
      this.pushEnabled.set(true);
    }
  }

  ngAfterViewInit(): void {
    this.observer = new IntersectionObserver(entries => {
      if (entries[0].isIntersecting) {
        this.store.loadMorePastEvents();
      }
    });
  }

  ngOnDestroy(): void {
    this.observer?.disconnect();
  }

  switchTab(tab: 'upcoming' | 'past'): void {
    this.activeTab.set(tab);
    if (tab === 'past' && this.store.pastEvents().length === 0) {
      this.store.loadPastEvents();
    }
    if (tab === 'past') {
      // Give Angular one tick to render the sentinel before observing
      setTimeout(() => {
        if (this.sentinel?.nativeElement) {
          this.observer?.observe(this.sentinel.nativeElement);
        }
      }, 0);
    }
  }

  protected openEventDetail(id: string): void {
    this.router.navigate(['/', id, 'home-event']);
  }

  async enablePush(): Promise<void> {
    await this.pushService.enableAndRegister();
    this.pushEnabled.set(true);
    localStorage.setItem('louvor4_push_enabled', 'true');
  }
}
```

- [ ] **Step 2: Build to verify**

```bash
cd /home/erivaldo/repositorio/erivaldo/louvor4-web
ng build 2>&1 | tail -10
```

Expected: `Application bundle generation complete.`

- [ ] **Step 3: Commit**

```bash
git add src/app/pages/home/home.component.ts
git commit -m "feat: adiciona abas e IntersectionObserver no HomeComponent"
```

---

### Task 8: HomeComponent HTML — tab bar and past events section

**Files:**
- Modify: `src/app/pages/home/home.component.html`

- [ ] **Step 1: Replace home.component.html**

```html
<div class="mx-auto w-full">
  @if (!pushEnabled()) {
  <div class="flex justify-center mb-6">
    <button (click)="enablePush()" class="inline-flex items-center gap-3 rounded-2xl
                   bg-gray-100 dark:bg-white/10
                   px-5 py-3 text-sm font-semibold
                   text-gray-700 dark:text-gray-200
                   hover:bg-gray-200 dark:hover:bg-white/20
                   transition-all duration-200 shadow-sm">
      <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5"
          stroke="currentColor" class="w-5 h-5 text-yellow-500">
        <path stroke-linecap="round" stroke-linejoin="round"
            d="M14.857 17.082a23.848 23.848 0 0 0 5.454-1.31A8.967 8.967 0 0 1 18 9.75V9A6 6 0 0 0 6 9v.75a8.967 8.967 0 0 1-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 0 1-5.714 0m5.714 0a3 3 0 1 1-5.714 0M3.124 7.5A8.969 8.969 0 0 1 5.292 3m13.416 0a8.969 8.969 0 0 1 2.168 4.5" />
      </svg>
      Ativar notificações
    </button>
  </div>
  }

  <div class="mb-6 flex items-center justify-between gap-4">
    <div class="min-w-0">
      <h1 class="text-xl sm:text-2xl font-black text-gray-900 dark:text-white tracking-tight truncate">
        Meus Eventos
      </h1>
      <p class="text-[10px] sm:text-sm font-medium text-gray-500 dark:text-gray-400 truncate">
        Acompanhe as escalas e apresentações que você faz parte
      </p>
    </div>
    @if (activeTab() === 'upcoming' && musicEvents().length > 0) {
    <span class="bg-blue-100 text-blue-600 dark:bg-blue-900/30 dark:text-blue-400 text-xs font-bold px-3 py-1 rounded-full">
      {{ musicEvents().length }}
    </span>
    }
  </div>

  <!-- Tab bar -->
  <div class="flex gap-4 mb-6 border-b border-gray-200 dark:border-white/10">
    <button
      (click)="switchTab('upcoming')"
      class="pb-2 px-1 text-sm font-semibold transition-colors duration-200"
      [class.text-brand-600]="activeTab() === 'upcoming'"
      [class.border-b-2]="activeTab() === 'upcoming'"
      [class.border-brand-600]="activeTab() === 'upcoming'"
      [class.text-gray-500]="activeTab() !== 'upcoming'"
      [class.dark:text-gray-400]="activeTab() !== 'upcoming'">
      Próximos
    </button>
    <button
      (click)="switchTab('past')"
      class="pb-2 px-1 text-sm font-semibold transition-colors duration-200"
      [class.text-brand-600]="activeTab() === 'past'"
      [class.border-b-2]="activeTab() === 'past'"
      [class.border-brand-600]="activeTab() === 'past'"
      [class.text-gray-500]="activeTab() !== 'past'"
      [class.dark:text-gray-400]="activeTab() !== 'past'">
      Passados
    </button>
  </div>

  <!-- Upcoming tab -->
  @if (activeTab() === 'upcoming') {
    @if (isLoading()) {
    <div class="grid grid-cols-1 gap-4">
      @for (i of [1, 2, 3]; track i) {
      <div class="relative flex flex-row items-center gap-3 rounded-[1.25rem] bg-white/50 p-2.5 
                          backdrop-blur-md shadow-sm border border-gray-100/50 
                          dark:bg-gray-900/30 dark:border-white/5 animate-pulse">
        <div class="min-w-[64px] h-[64px] rounded-[1rem] bg-gray-200 dark:bg-white/5"></div>
        <div class="flex flex-1 items-center gap-3.5 min-w-0">
          <div class="h-14 w-14 shrink-0 rounded-[1rem] bg-gray-200 dark:bg-white/5"></div>
          <div class="flex flex-col min-w-0 flex-1 justify-center gap-2">
            <div class="h-4 w-3/4 bg-gray-200 dark:bg-white/5 rounded-md"></div>
            <div class="h-3 w-1/2 bg-gray-200 dark:bg-white/5 rounded-md"></div>
          </div>
        </div>
        <div class="flex flex-col items-end gap-2.5 px-3 border-l border-gray-100 dark:border-white/5 min-w-[50px] justify-center">
          <div class="h-4 w-6 bg-gray-200 dark:bg-white/5 rounded-md"></div>
          <div class="h-4 w-6 bg-gray-200 dark:bg-white/5 rounded-md"></div>
        </div>
      </div>
      }
    </div>
    } @else if (musicEvents().length > 0) {
    <div class="grid grid-cols-1 gap-4">
      @for (event of musicEvents(); track event.id; let i = $index) {
      <button type="button"
          class="group relative w-full text-left transition-all duration-300 hover:z-10 focus:outline-none opacity-0 animate-[fade-in-up_0.5s_ease-out_forwards]"
          [style.animation-delay]="(i * 100) + 'ms'" (click)="openEventDetail(event.id)">
        <app-music-event-list-item [event]="event" class="block"></app-music-event-list-item>
      </button>
      }
    </div>
    } @else {
    <div class="flex flex-col items-center justify-center py-20 px-6 bg-gradient-to-b from-white/60 to-white/10 dark:from-white/5 dark:to-transparent rounded-[2rem] border-[1.5px] border-dashed border-gray-200/80 dark:border-white/10 shadow-sm backdrop-blur-md">
      <div class="relative p-5 bg-brand-50 dark:bg-brand-500/10 rounded-full mb-6 group transition-transform hover:scale-110 duration-500">
        <div class="absolute inset-0 bg-brand-400/20 dark:bg-brand-400/10 rounded-full blur-xl animate-pulse"></div>
        <svg xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
            class="text-brand-600 dark:text-brand-400 relative z-10">
          <path d="M8 2v4" />
          <path d="M16 2v4" />
          <rect width="18" height="18" x="3" y="4" rx="2" />
          <path d="M3 10h18" />
          <path d="M8 14h.01" />
          <path d="M12 14h.01" />
          <path d="M16 14h.01" />
          <path d="M8 18h.01" />
          <path d="M12 18h.01" />
          <path d="M16 18h.01" />
        </svg>
      </div>
      <h3 class="text-gray-900 dark:text-white font-black text-2xl mb-2 tracking-tight">Tudo calmo por aqui</h3>
      <p class="text-gray-500 dark:text-gray-400 text-sm max-w-[280px] text-center font-medium leading-relaxed mb-8">
        Você não possui eventos agendados no momento. Que tal procurar novos projetos ou criar um evento?
      </p>
      <a routerLink="/music-project"
          class="inline-flex items-center gap-2 px-6 py-3 bg-brand-600 hover:bg-brand-500 text-white rounded-xl font-bold shadow-lg shadow-brand-500/30 transition-transform active:scale-95 duration-200 cursor-pointer">
        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round">
          <path d="m18 15-6-6-6 6" />
        </svg>
        Explorar Projetos
      </a>
    </div>
    }
  }

  <!-- Past events tab -->
  @if (activeTab() === 'past') {
    @if (store.isPastLoading()) {
    <div class="grid grid-cols-1 gap-4">
      @for (i of [1, 2, 3]; track i) {
      <div class="relative flex flex-row items-center gap-3 rounded-[1.25rem] bg-white/50 p-2.5 
                          backdrop-blur-md shadow-sm border border-gray-100/50 
                          dark:bg-gray-900/30 dark:border-white/5 animate-pulse">
        <div class="min-w-[64px] h-[64px] rounded-[1rem] bg-gray-200 dark:bg-white/5"></div>
        <div class="flex flex-1 items-center gap-3.5 min-w-0">
          <div class="h-14 w-14 shrink-0 rounded-[1rem] bg-gray-200 dark:bg-white/5"></div>
          <div class="flex flex-col min-w-0 flex-1 justify-center gap-2">
            <div class="h-4 w-3/4 bg-gray-200 dark:bg-white/5 rounded-md"></div>
            <div class="h-3 w-1/2 bg-gray-200 dark:bg-white/5 rounded-md"></div>
          </div>
        </div>
        <div class="flex flex-col items-end gap-2.5 px-3 border-l border-gray-100 dark:border-white/5 min-w-[50px] justify-center">
          <div class="h-4 w-6 bg-gray-200 dark:bg-white/5 rounded-md"></div>
          <div class="h-4 w-6 bg-gray-200 dark:bg-white/5 rounded-md"></div>
        </div>
      </div>
      }
    </div>
    } @else if (store.pastEvents().length > 0) {
    <div class="grid grid-cols-1 gap-4">
      @for (event of store.pastEvents(); track event.id; let i = $index) {
      <button type="button"
          class="group relative w-full text-left transition-all duration-300 hover:z-10 focus:outline-none opacity-0 animate-[fade-in-up_0.5s_ease-out_forwards]"
          [style.animation-delay]="(i * 100) + 'ms'" (click)="openEventDetail(event.id)">
        <app-music-event-list-item [event]="event" class="block"></app-music-event-list-item>
      </button>
      }
    </div>
    <div #sentinel class="h-4"></div>
    @if (store.isLoadingMore()) {
    <div class="flex justify-center py-4">
      <div class="w-6 h-6 rounded-full border-2 border-brand-600 border-t-transparent animate-spin"></div>
    </div>
    }
    } @else {
    <div class="flex flex-col items-center justify-center py-20 px-6 bg-gradient-to-b from-white/60 to-white/10 dark:from-white/5 dark:to-transparent rounded-[2rem] border-[1.5px] border-dashed border-gray-200/80 dark:border-white/10 shadow-sm backdrop-blur-md">
      <div class="relative p-5 bg-brand-50 dark:bg-brand-500/10 rounded-full mb-6">
        <div class="absolute inset-0 bg-brand-400/20 dark:bg-brand-400/10 rounded-full blur-xl animate-pulse"></div>
        <svg xmlns="http://www.w3.org/2000/svg" width="36" height="36" viewBox="0 0 24 24" fill="none"
            stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
            class="text-brand-600 dark:text-brand-400 relative z-10">
          <path d="M12 8v4l3 3" />
          <circle cx="12" cy="12" r="9" />
        </svg>
      </div>
      <h3 class="text-gray-900 dark:text-white font-black text-2xl mb-2 tracking-tight">Nenhum evento anterior</h3>
      <p class="text-gray-500 dark:text-gray-400 text-sm max-w-[280px] text-center font-medium leading-relaxed">
        Você ainda não participou de nenhum evento.
      </p>
    </div>
    }
  }
</div>
```

- [ ] **Step 2: Build to verify**

```bash
cd /home/erivaldo/repositorio/erivaldo/louvor4-web
ng build 2>&1 | tail -10
```

Expected: `Application bundle generation complete.`

- [ ] **Step 3: Commit**

```bash
git add src/app/pages/home/home.component.html
git commit -m "feat: adiciona aba de eventos passados com infinite scroll no HomeComponent"
```

---

## Self-Review

**Spec coverage:**
- ✅ `GET /me/events` inalterado — `getEventsByUser` não foi tocado
- ✅ `GET /me/events/past?page=0&size=10` — Task 4
- ✅ Query `startAt < now ORDER BY startAt desc` — Task 1
- ✅ `EventParticipantStatus.ACCEPTED` passado — Task 3
- ✅ `Page<UserEventDetailDto>` com `content`, `last`, `totalElements` — Tasks 3–4
- ✅ `HomeStore.pastEvents`, `pastPage`, `pastHasMore`, `isLoadingMore`, `isPastLoading` — Task 6
- ✅ `loadPastEvents()` reseta estado e carrega página 0 — Task 6
- ✅ `loadMorePastEvents()` guard + acumula — Task 6
- ✅ `activeTab` signal + `switchTab()` — Task 7
- ✅ `IntersectionObserver` conectado ao sentinel — Task 7
- ✅ `ngOnDestroy` desconecta observer — Task 7
- ✅ Tab bar Próximos/Passados — Task 8
- ✅ Skeleton loading para passados — Task 8
- ✅ Empty state "Nenhum evento anterior" — Task 8
- ✅ Sentinel `<div #sentinel>` + spinner `isLoadingMore` — Task 8
- ✅ Frontend carrega passados só na primeira ativação da aba — Task 7 (`pastEvents().length === 0` guard)
- ✅ Falha no `loadMore` não avança `pastPage`, reseta `isLoadingMore` — Task 6
- ✅ `countQuery` separado para evitar problema de Hibernate com `join fetch` + `Page` — Task 1

**Placeholder scan:** nenhum TBD/TODO encontrado.

**Type consistency:**
- `findPastByUserWithEventAndProjectAndMemberUser` — definido no Task 1, usado nos Tasks 2 e 3 com a mesma assinatura
- `getPastEventsByUser(Pageable)` — definido na interface no Task 3 step 1, implementado no Task 3 step 3, exposto no Task 4
- `getPastEvents(page, size)` — definido no Task 5, consumido no Task 6
- `store.isPastLoading()`, `store.pastEvents()`, `store.isLoadingMore()` — definidos no Task 6, usados no Task 8
