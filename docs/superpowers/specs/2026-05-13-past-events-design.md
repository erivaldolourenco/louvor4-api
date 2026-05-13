# Design: Eventos Passados com Infinite Scroll

**Data:** 2026-05-13
**Status:** Aprovado

---

## Contexto

A tela inicial do aplicativo exibe "Meus Eventos", mas filtra apenas eventos futuros (`startAt >= now`). O usuário não consegue ver o histórico dos eventos em que participou. O objetivo é adicionar duas abas na home — "Próximos" (padrão, comportamento atual) e "Passados" — onde os eventos passados são carregados sob demanda via infinite scroll.

---

## Requisitos

- Aba "Próximos": comportamento idêntico ao atual, sem nenhuma alteração
- Aba "Passados": lista paginada dos eventos com `startAt < now` onde o usuário foi participante `ACCEPTED`, ordenada do mais recente para o mais antigo
- Infinite scroll: ao rolar até o fim da lista, a próxima página é carregada automaticamente via `IntersectionObserver`
- Endpoint de futuros (`GET /me/events`) não é alterado
- O frontend só carrega eventos passados quando o usuário ativa a aba pela primeira vez

---

## Arquitetura

Dois fluxos independentes:

```
GET /me/events            → inalterado (futuros, sem paginação)
GET /me/events/past       → novo endpoint paginado
```

```
HomeComponent
  ├── aba "Próximos"  → HomeStore.upcomingEvents (existente)
  └── aba "Passados"  → HomeStore.pastEvents (novo, acumulativo)
                              └── IntersectionObserver (sentinel)
                                     └── loadMorePastEvents()
```

---

## Backend

### Nova query — `EventParticipantRepository`

```java
@Query("""
    select ep from EventParticipant ep
    join fetch ep.event e
    join fetch e.musicProject
    join fetch ep.member m
    join fetch m.user u
    where u.id = :userId
      and ep.status = :status
      and e.startAt < :now
    order by e.startAt desc
    """)
Page<EventParticipant> findPastByUserWithEventAndProjectAndMemberUser(
    @Param("userId") UUID userId,
    @Param("status") EventParticipantStatus status,
    @Param("now") LocalDateTime now,
    Pageable pageable);
```

### Novo método — `EventService`

```java
Page<UserEventDetailDto> getPastEventsByUser(Pageable pageable);
```

### Implementação — `EventServiceImpl.getPastEventsByUser`

Mesma lógica de montagem de DTO do método `getEventsByUser` existente:
1. Busca `Page<EventParticipant>` via nova query com `now = LocalDateTime.now()`
2. Extrai eventos distintos do conteúdo da página
3. Busca contagem de participantes e fotos de perfil por `eventIds`
4. Monta `UserEventDetailDto` para cada evento
5. Retorna `PageImpl<UserEventDetailDto>` com o mesmo `Pageable` e `totalElements` da query original

### Novo endpoint — `UserController`

```
GET /me/events/past?page=0&size=10

Authorization: Bearer {token}
```

```java
@GetMapping("/events/past")
public ResponseEntity<Page<UserEventDetailDto>> getPastEventsByUser(
    @PageableDefault(size = 10, sort = "startAt", direction = Sort.Direction.DESC) Pageable pageable) {
    return ResponseEntity.ok(eventService.getPastEventsByUser(pageable));
}
```

**Response 200:**
```json
{
  "content": [ ...UserEventDetailDto ],
  "last": true,
  "totalElements": 42
}
```

---

## Frontend

### Novo modelo — `Page<T>`

```ts
// src/app/core/models/page.model.ts
export interface Page<T> {
  content: T[];
  last: boolean;
  totalElements: number;
}
```

### `user.service.ts` — novo método

```ts
getPastEvents(page: number, size = 10): Observable<Page<MusicEventDetail>> {
  return this.http.get<Page<MusicEventDetail>>(
    `${this.baseUrl}/events/past?page=${page}&size=${size}`
  );
}
```

### `HomeStore` — estado adicionado

```ts
// Estado existente (inalterado)
events       = signal<MusicEventDetail[]>([]);
isLoading    = signal<boolean>(true);

// Estado novo para passados
pastEvents    = signal<MusicEventDetail[]>([]);
pastPage      = signal(0);
pastHasMore   = signal(true);
isLoadingMore = signal(false);
isPastLoading = signal(false);

loadPastEvents(): void      // carrega página 0, reseta lista
loadMorePastEvents(): void  // carrega próxima página, acumula
```

- `loadPastEvents()`: define `pastPage(0)`, `pastHasMore(true)`, `pastEvents([])`, então chama `userService.getPastEvents(0)` e popula `pastEvents` com `content`. Marca `pastHasMore` conforme `page.last`.
- `loadMorePastEvents()`: guard `if (!pastHasMore() || isLoadingMore()) return`. Incrementa `pastPage`, chama `getPastEvents(pastPage())`, concatena resultado em `pastEvents`, atualiza `pastHasMore`.

### `home.component.ts` — mudanças

```ts
activeTab = signal<'upcoming' | 'past'>('upcoming');
@ViewChild('sentinel') sentinel!: ElementRef;
private observer?: IntersectionObserver;

switchTab(tab: 'upcoming' | 'past'): void
  // se tab === 'past' && pastEvents().length === 0 → store.loadPastEvents()
  // (re)conecta IntersectionObserver ao sentinel

ngAfterViewInit(): void
  // configura IntersectionObserver

ngOnDestroy(): void
  // desconecta observer
```

`IntersectionObserver` callback: `if (entry.isIntersecting) store.loadMorePastEvents()`

### `home.component.html` — estrutura

```
Barra de abas (Próximos / Passados)

@if (activeTab() === 'upcoming') {
  lista atual (sem mudança alguma)
}

@if (activeTab() === 'past') {
  @if (isPastLoading()) { skeleton }
  @else if (pastEvents().length > 0) {
    @for (event of pastEvents()) {
      <app-music-event-list-item>
    }
    <div #sentinel class="h-4"></div>
    @if (isLoadingMore()) { spinner }
  }
  @else { empty state "Nenhum evento anterior" }
}
```

O estilo das abas, skeleton e cards segue o padrão Tailwind + dark mode já presente no componente.

---

## Tratamento de erros

- Falha no carregamento inicial da aba passados: exibe estado de erro com botão "Tentar novamente"
- Falha no `loadMore`: não avança `pastPage`, mantém lista atual, loga o erro (sem toast — o usuário pode rolar para cima e tentar novamente ao voltar ao fim da lista)
- `isLoadingMore` é sempre resetado para `false` tanto no `next` quanto no `error`

---

## O que não muda

- `GET /me/events` e seu comportamento
- `HomeStore.load()` e `HomeStore.events`
- `MusicEventListItemComponent`
- Roteamento e navegação ao clicar em um evento
