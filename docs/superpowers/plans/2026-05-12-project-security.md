# Project Security (ABAC) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Garantir que apenas membros ACTIVE de um projeto possam acessar seus dados, usando um bean `ProjectSecurity` + `@PreAuthorize` em todos os endpoints sensíveis.

**Architecture:** Padrão ABAC (Attribute-Based Access Control) via Spring Method Security. Um bean `@Component("projectSecurity")` centraliza toda lógica de autorização por contexto de projeto. Os controllers recebem `@PreAuthorize("@projectSecurity.isMember(#projectId)")` ou `@PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")`. Para endpoints de eventos (que recebem `eventId` em vez de `projectId`), o bean resolve o projectId via `EventRepository`. O `@EnableMethodSecurity` já está habilitado em `SecurityConfig`.

**Tech Stack:** Java 21, Spring Boot 3, Spring Security 6, JUnit 5, Mockito

---

## Mapa de arquivos

| Ação | Arquivo |
|---|---|
| Modify | `src/main/java/br/com/louvor4/api/repositories/EventRepository.java` |
| Create | `src/main/java/br/com/louvor4/api/config/security/ProjectSecurity.java` |
| Modify | `src/main/java/br/com/louvor4/api/controllers/MusicProjectController.java` |
| Modify | `src/main/java/br/com/louvor4/api/controllers/EventController.java` |
| Modify | `src/main/java/br/com/louvor4/api/controllers/EventProgramController.java` |
| Create | `src/test/java/br/com/louvor4/api/config/security/ProjectSecurityTest.java` |

---

### Task 1: Adicionar query `findProjectIdByEventId` ao EventRepository

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/repositories/EventRepository.java`

O `ProjectSecurity` precisa resolver o `projectId` de um evento sem carregar a entidade inteira.

- [ ] **Step 1: Adicionar o método JPQL ao EventRepository**

O arquivo atual tem imports de `@Query` e `@Param`. Adicionar o seguinte método após os existentes:

```java
@Query("SELECT e.musicProject.id FROM Event e WHERE e.id = :eventId")
UUID findProjectIdByEventId(@Param("eventId") UUID eventId);
```

O arquivo completo deve ficar:

```java
package br.com.louvor4.api.repositories;

import br.com.louvor4.api.enums.SetlistItemType;
import br.com.louvor4.api.models.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findAllByMusicProject_IdAndStartAtGreaterThanEqualOrderByStartAtAsc(UUID projectId, LocalDateTime now);

    List<Event> findAllByMusicProjectIdAndStartAtBetweenOrderByStartAtAsc(UUID projectId, LocalDateTime start, LocalDateTime end);

    @Query("""
        select count(distinct ep.member.id)
        from EventParticipant ep
        where ep.event.id = :eventId
    """)
    Integer countParticipantsByEventId(@Param("eventId") UUID eventId);

    @Query("""
        select count(distinct es.song.id)
        from EventSetlistItem es
        where es.event.id = :eventId
          and es.type = :type
          and es.song is not null
    """)
    Integer countSongsByEventId(@Param("eventId") UUID eventId, @Param("type") SetlistItemType type);

    @Query("SELECT e.musicProject.id FROM Event e WHERE e.id = :eventId")
    UUID findProjectIdByEventId(@Param("eventId") UUID eventId);
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/br/com/louvor4/api/repositories/EventRepository.java
git commit -m "feat: adiciona findProjectIdByEventId ao EventRepository"
```

---

### Task 2: Criar o bean `ProjectSecurity`

**Files:**
- Create: `src/main/java/br/com/louvor4/api/config/security/ProjectSecurity.java`

Este é o bean central de autorização. Encapsula toda a lógica de verificação de membership por projeto.

- [ ] **Step 1: Criar o arquivo**

```java
package br.com.louvor4.api.config.security;

import br.com.louvor4.api.enums.ProjectMemberRole;
import br.com.louvor4.api.enums.ProjectMemberStatus;
import br.com.louvor4.api.repositories.EventRepository;
import br.com.louvor4.api.repositories.MusicProjectMemberRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("projectSecurity")
public class ProjectSecurity {

    private final MusicProjectMemberRepository memberRepository;
    private final CurrentUserProvider currentUserProvider;
    private final EventRepository eventRepository;

    public ProjectSecurity(MusicProjectMemberRepository memberRepository,
                           CurrentUserProvider currentUserProvider,
                           EventRepository eventRepository) {
        this.memberRepository = memberRepository;
        this.currentUserProvider = currentUserProvider;
        this.eventRepository = eventRepository;
    }

    public boolean isMember(UUID projectId) {
        UUID userId = currentUserProvider.get().getId();
        return memberRepository.existsByMusicProject_IdAndUser_IdAndStatus(
                projectId, userId, ProjectMemberStatus.ACTIVE);
    }

    public boolean isAdminOrOwner(UUID projectId) {
        UUID userId = currentUserProvider.get().getId();
        return memberRepository.findByMusicProject_IdAndUser_IdAndStatus(
                        projectId, userId, ProjectMemberStatus.ACTIVE)
                .map(m -> m.getProjectRole() == ProjectMemberRole.OWNER
                        || m.getProjectRole() == ProjectMemberRole.ADMIN)
                .orElse(false);
    }

    public boolean isMemberByEventId(UUID eventId) {
        UUID projectId = eventRepository.findProjectIdByEventId(eventId);
        if (projectId == null) return false;
        return isMember(projectId);
    }

    public boolean isAdminOrOwnerByEventId(UUID eventId) {
        UUID projectId = eventRepository.findProjectIdByEventId(eventId);
        if (projectId == null) return false;
        return isAdminOrOwner(projectId);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/br/com/louvor4/api/config/security/ProjectSecurity.java
git commit -m "feat: cria bean ProjectSecurity para autorização ABAC por projeto"
```

---

### Task 3: Testes unitários para `ProjectSecurity`

**Files:**
- Create: `src/test/java/br/com/louvor4/api/config/security/ProjectSecurityTest.java`

- [ ] **Step 1: Criar o arquivo de teste**

```java
package br.com.louvor4.api.config.security;

import br.com.louvor4.api.enums.ProjectMemberRole;
import br.com.louvor4.api.enums.ProjectMemberStatus;
import br.com.louvor4.api.models.MusicProjectMember;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.EventRepository;
import br.com.louvor4.api.repositories.MusicProjectMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectSecurityTest {

    @Mock MusicProjectMemberRepository memberRepository;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock EventRepository eventRepository;

    @InjectMocks ProjectSecurity projectSecurity;

    private UUID projectId;
    private UUID userId;
    private UUID eventId;
    private User user;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
        eventId = UUID.randomUUID();

        user = new User();
        user.setId(userId);

        when(currentUserProvider.get()).thenReturn(user);
    }

    // --- isMember ---

    @Test
    void isMemberShouldReturnTrueWhenUserIsActiveMember() {
        when(memberRepository.existsByMusicProject_IdAndUser_IdAndStatus(
                projectId, userId, ProjectMemberStatus.ACTIVE)).thenReturn(true);

        assertThat(projectSecurity.isMember(projectId)).isTrue();
    }

    @Test
    void isMemberShouldReturnFalseWhenUserIsNotMember() {
        when(memberRepository.existsByMusicProject_IdAndUser_IdAndStatus(
                projectId, userId, ProjectMemberStatus.ACTIVE)).thenReturn(false);

        assertThat(projectSecurity.isMember(projectId)).isFalse();
    }

    // --- isAdminOrOwner ---

    @Test
    void isAdminOrOwnerShouldReturnTrueForOwner() {
        MusicProjectMember member = new MusicProjectMember();
        member.setProjectRole(ProjectMemberRole.OWNER);

        when(memberRepository.findByMusicProject_IdAndUser_IdAndStatus(
                projectId, userId, ProjectMemberStatus.ACTIVE)).thenReturn(Optional.of(member));

        assertThat(projectSecurity.isAdminOrOwner(projectId)).isTrue();
    }

    @Test
    void isAdminOrOwnerShouldReturnTrueForAdmin() {
        MusicProjectMember member = new MusicProjectMember();
        member.setProjectRole(ProjectMemberRole.ADMIN);

        when(memberRepository.findByMusicProject_IdAndUser_IdAndStatus(
                projectId, userId, ProjectMemberStatus.ACTIVE)).thenReturn(Optional.of(member));

        assertThat(projectSecurity.isAdminOrOwner(projectId)).isTrue();
    }

    @Test
    void isAdminOrOwnerShouldReturnFalseForRegularMember() {
        MusicProjectMember member = new MusicProjectMember();
        member.setProjectRole(ProjectMemberRole.MEMBER);

        when(memberRepository.findByMusicProject_IdAndUser_IdAndStatus(
                projectId, userId, ProjectMemberStatus.ACTIVE)).thenReturn(Optional.of(member));

        assertThat(projectSecurity.isAdminOrOwner(projectId)).isFalse();
    }

    @Test
    void isAdminOrOwnerShouldReturnFalseWhenNotMember() {
        when(memberRepository.findByMusicProject_IdAndUser_IdAndStatus(
                projectId, userId, ProjectMemberStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThat(projectSecurity.isAdminOrOwner(projectId)).isFalse();
    }

    // --- isMemberByEventId ---

    @Test
    void isMemberByEventIdShouldReturnTrueWhenUserIsMemberOfEventProject() {
        when(eventRepository.findProjectIdByEventId(eventId)).thenReturn(projectId);
        when(memberRepository.existsByMusicProject_IdAndUser_IdAndStatus(
                projectId, userId, ProjectMemberStatus.ACTIVE)).thenReturn(true);

        assertThat(projectSecurity.isMemberByEventId(eventId)).isTrue();
    }

    @Test
    void isMemberByEventIdShouldReturnFalseWhenEventNotFound() {
        when(eventRepository.findProjectIdByEventId(eventId)).thenReturn(null);

        assertThat(projectSecurity.isMemberByEventId(eventId)).isFalse();
    }

    // --- isAdminOrOwnerByEventId ---

    @Test
    void isAdminOrOwnerByEventIdShouldReturnTrueWhenOwnerOfEventProject() {
        MusicProjectMember member = new MusicProjectMember();
        member.setProjectRole(ProjectMemberRole.OWNER);

        when(eventRepository.findProjectIdByEventId(eventId)).thenReturn(projectId);
        when(memberRepository.findByMusicProject_IdAndUser_IdAndStatus(
                projectId, userId, ProjectMemberStatus.ACTIVE)).thenReturn(Optional.of(member));

        assertThat(projectSecurity.isAdminOrOwnerByEventId(eventId)).isTrue();
    }

    @Test
    void isAdminOrOwnerByEventIdShouldReturnFalseWhenEventNotFound() {
        when(eventRepository.findProjectIdByEventId(eventId)).thenReturn(null);

        assertThat(projectSecurity.isAdminOrOwnerByEventId(eventId)).isFalse();
    }
}
```

- [ ] **Step 2: Rodar os testes**

```bash
cd /home/erivaldo/repositorio/erivaldo/louvor4-api && ./mvnw test -Dtest=ProjectSecurityTest -q 2>&1 | tail -15
```

Saída esperada: `Tests run: 9, Failures: 0, Errors: 0` e `BUILD SUCCESS`.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/br/com/louvor4/api/config/security/ProjectSecurityTest.java
git commit -m "test: adiciona testes unitários para ProjectSecurity"
```

---

### Task 4: Aplicar `@PreAuthorize` no `MusicProjectController`

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/controllers/MusicProjectController.java`

Adicionar `@PreAuthorize` em todos os endpoints que acessam dados de um projeto específico. **Não adicionar** em: `POST /create` (qualquer autenticado pode criar), `GET /invites` (sem contexto de projeto), `POST /{projectId}/members/invite/respond` (usuário ainda não é membro quando responde ao convite).

**Regra:** leitura de dados do projeto → `isMember`; escrita/gerenciamento → `isAdminOrOwner`.

Adicionar o import no topo do arquivo (após os imports existentes):
```java
import org.springframework.security.access.prepost.PreAuthorize;
```

Mapeamento completo de cada endpoint e sua annotation:

| Método atual | `@PreAuthorize` |
|---|---|
| `GET /{id}` | `@PreAuthorize("@projectSecurity.isMember(#id)")` |
| `PUT /{id}` | `@PreAuthorize("@projectSecurity.isAdminOrOwner(#id)")` |
| `PUT /{id}/profile-image` | `@PreAuthorize("@projectSecurity.isAdminOrOwner(#id)")` |
| `POST /{projectId}/members` | `@PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")` |
| `GET /{projectId}/members` | `@PreAuthorize("@projectSecurity.isMember(#projectId)")` |
| `GET /{projectId}/members/{memberId}` | `@PreAuthorize("@projectSecurity.isMember(#projectId)")` |
| `PUT /{projectId}/members/{memberId}` | `@PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")` |
| `DELETE /{projectId}/members/{memberId}` | `@PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")` |
| `POST /{projectId}/events` | `@PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")` |
| `GET /{projectId}/events` | `@PreAuthorize("@projectSecurity.isMember(#projectId)")` |
| `POST /{projectId}/skills` | `@PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")` |
| `GET /{projectId}/skills` | `@PreAuthorize("@projectSecurity.isMember(#projectId)")` |
| `POST /{projectId}/members/{memberId}/skills` | `@PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")` |
| `GET /{projectId}/member-role` | `@PreAuthorize("@projectSecurity.isMember(#projectId)")` |
| `GET /{projectId}/months/{yearMonth}/overview` | `@PreAuthorize("@projectSecurity.isMember(#projectId)")` |

- [ ] **Step 1: Substituir o conteúdo completo do controller**

```java
package br.com.louvor4.api.controllers;

import br.com.louvor4.api.enums.ProjectMemberRole;
import br.com.louvor4.api.services.MusicProjectService;
import br.com.louvor4.api.shared.dto.Event.CreateEventDto;
import br.com.louvor4.api.shared.dto.Event.EventDetailDto;
import br.com.louvor4.api.shared.dto.MusicProject.*;
import br.com.louvor4.api.shared.dto.MusicProject.ProjectInviteDTO;
import br.com.louvor4.api.shared.dto.MusicProject.ProjectInviteResponseDTO;
import br.com.louvor4.api.shared.dto.eventOverview.MonthOverviewResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("music-project")
public class MusicProjectController {

    private final MusicProjectService musicProjectService;

    public MusicProjectController(MusicProjectService musicProjectService) {
        this.musicProjectService = musicProjectService;
    }

    @PostMapping("/create")
    public ResponseEntity<MusicProjectDetailDTO> create(@RequestBody @Valid MusicProjectCreateDTO createDto) {
        MusicProjectDetailDTO dto = musicProjectService.create(createDto);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@projectSecurity.isMember(#id)")
    public ResponseEntity<MusicProjectDetailDTO> findById(@PathVariable UUID id) {
        MusicProjectDetailDTO musicProjectDetailDTO = musicProjectService.getById(id);
        return ResponseEntity.ok(musicProjectDetailDTO);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@projectSecurity.isAdminOrOwner(#id)")
    public ResponseEntity<MusicProjectDetailDTO> update(@PathVariable UUID id, @RequestBody @Valid MusicProjectDTO updateDto) {
        MusicProjectDetailDTO musicProjectDetailDTO = musicProjectService.update(id, updateDto);
        return ResponseEntity.ok(musicProjectDetailDTO);
    }

    @PutMapping(value = "/{id}/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("@projectSecurity.isAdminOrOwner(#id)")
    public ResponseEntity<String> updateProfileImage(
            @PathVariable UUID id,
            @RequestPart("profileImage") MultipartFile profileImage
    ) {
        String url = musicProjectService.updateImage(id, profileImage);
        return ResponseEntity.ok(url);
    }

    @PostMapping("/{projectId}/members")
    @PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")
    public ResponseEntity<Void> addMember(@PathVariable UUID projectId, @RequestBody @Valid AddMemberDTO addDto) {
        musicProjectService.addMember(projectId, addDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/invites")
    public ResponseEntity<List<ProjectInviteDTO>> getMyInvites() {
        return ResponseEntity.ok(musicProjectService.getMyInvites());
    }

    @PostMapping("/{projectId}/members/invite/respond")
    public ResponseEntity<Void> respondInvite(
            @PathVariable UUID projectId,
            @RequestBody @Valid ProjectInviteResponseDTO responseDto) {
        musicProjectService.respondInvite(projectId, responseDto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{projectId}/members")
    @PreAuthorize("@projectSecurity.isMember(#projectId)")
    public ResponseEntity<List<MemberDTO>> getMembers(@PathVariable UUID projectId) {
        List<MemberDTO> members = musicProjectService.getMembers(projectId);
        return ResponseEntity.ok(members);
    }

    @GetMapping("/{projectId}/members/{memberId}")
    @PreAuthorize("@projectSecurity.isMember(#projectId)")
    public ResponseEntity<MemberDTO> getMember(@PathVariable UUID projectId, @PathVariable UUID memberId) {
        MemberDTO members = musicProjectService.getMember(projectId, memberId);
        return ResponseEntity.ok(members);
    }

    @PutMapping("/{projectId}/members/{memberId}")
    @PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")
    public ResponseEntity<MemberDTO> updateMember(@PathVariable UUID projectId, @PathVariable UUID memberId, @RequestBody UpdateMemberRequest request) {
        MemberDTO members = musicProjectService.updateMember(projectId, memberId, request);
        return ResponseEntity.ok(members);
    }

    @DeleteMapping("/{projectId}/members/{memberId}")
    @PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")
    public ResponseEntity<Void> deleteMember(@PathVariable UUID projectId, @PathVariable UUID memberId) {
        musicProjectService.deleteMember(projectId, memberId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{projectId}/events")
    @PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")
    public ResponseEntity<Void> createEvent(@PathVariable UUID projectId, @RequestBody @Valid CreateEventDto eventDto) {
        musicProjectService.createEvent(projectId, eventDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{projectId}/events")
    @PreAuthorize("@projectSecurity.isMember(#projectId)")
    public ResponseEntity<List<EventDetailDto>> getEventsByProject(@PathVariable UUID projectId) {
        return ResponseEntity.ok(musicProjectService.getEventsByProject(projectId));
    }

    @PostMapping("/{projectId}/skills")
    @PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")
    public ResponseEntity<Void> addProjectSkill(@PathVariable UUID projectId, @RequestBody @Valid ProjectSkillRequestDTO skillDto) {
        musicProjectService.addProjectSkill(projectId, skillDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @GetMapping("/{projectId}/skills")
    @PreAuthorize("@projectSecurity.isMember(#projectId)")
    public ResponseEntity<List<ProjectSkillDTO>> getProjectSkills(@PathVariable UUID projectId) {
        List<ProjectSkillDTO> skills = musicProjectService.getProjectSkills(projectId);
        return ResponseEntity.ok(skills);
    }

    @PostMapping("/{projectId}/members/{memberId}/skills")
    @PreAuthorize("@projectSecurity.isAdminOrOwner(#projectId)")
    public ResponseEntity<Void> assignSkillsToMember(
            @PathVariable UUID projectId,
            @PathVariable UUID memberId,
            @RequestBody List<UUID> skillIds) {
        musicProjectService.assignSkillsToMember(projectId, memberId, skillIds);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{projectId}/member-role")
    @PreAuthorize("@projectSecurity.isMember(#projectId)")
    public ResponseEntity<ProjectMemberRole> getMemberRole(@PathVariable UUID projectId) {
        ProjectMemberRole memberRole = musicProjectService.getMemberRole(projectId);
        return ResponseEntity.ok(memberRole);
    }

    @GetMapping("/{projectId}/months/{yearMonth}/overview")
    @PreAuthorize("@projectSecurity.isMember(#projectId)")
    public ResponseEntity<MonthOverviewResponse> getMonthOverview(@PathVariable UUID projectId, @PathVariable String yearMonth) {
        MonthOverviewResponse resp = musicProjectService.getMonthOverview(projectId, yearMonth);
        return ResponseEntity.ok(resp);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/br/com/louvor4/api/controllers/MusicProjectController.java
git commit -m "feat: aplica @PreAuthorize de membership em MusicProjectController"
```

---

### Task 5: Aplicar `@PreAuthorize` no `EventController`

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/controllers/EventController.java`

Endpoints de evento usam `eventId` em vez de `projectId`. O bean `projectSecurity` resolve via `isMemberByEventId` e `isAdminOrOwnerByEventId`.

**Exceções sem `@PreAuthorize` adicional:**
- `PATCH /participants/{participantId}/accept` — o participante responde para si mesmo; o service já valida o vínculo
- `PATCH /participants/{participantId}/decline` — mesmo caso

**Nota:** `GET /{id}` usa `{id}` como nome do path variable; usar `#id` na expressão.

- [ ] **Step 1: Substituir o conteúdo completo do controller**

```java
package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.EventService;
import br.com.louvor4.api.shared.dto.Event.*;
import br.com.louvor4.api.shared.dto.Song.AddEventSetlistItemDTO;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/{id}")
    @PreAuthorize("@projectSecurity.isMemberByEventId(#id)")
    public ResponseEntity<EventDetailDto> findById(@PathVariable UUID id) {
        EventDetailDto eventDetailDto = eventService.getEventById(id);
        return ResponseEntity.ok(eventDetailDto);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@projectSecurity.isAdminOrOwnerByEventId(#id)")
    public ResponseEntity<Void> deleteEventById(@PathVariable UUID id) {
        eventService.deleteEventById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{eventId}")
    @PreAuthorize("@projectSecurity.isAdminOrOwnerByEventId(#eventId)")
    public ResponseEntity<Void> updateEvent(@PathVariable UUID eventId, @RequestBody @Valid UpdateEventDto eventDto) {
        eventService.updateEventBy(eventId, eventDto);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}/participants")
    @PreAuthorize("@projectSecurity.isMemberByEventId(#eventId)")
    public ResponseEntity<List<EventParticipantResponseDTO>> getParticipants(@PathVariable UUID eventId) {
        return ResponseEntity.ok(eventService.getParticipants(eventId));
    }

    @PostMapping("/{eventId}/participants")
    @PreAuthorize("@projectSecurity.isAdminOrOwnerByEventId(#eventId)")
    public ResponseEntity<Void> addOrUpdateParticipant(@PathVariable UUID eventId, @RequestBody @Valid List<EventParticipantDTO> participantDto) {
        eventService.addOrUpdateParticipantsToEvent(eventId, participantDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PatchMapping("/participants/{participantId}/accept")
    public ResponseEntity<Void> acceptParticipation(@PathVariable UUID participantId) {
        eventService.acceptParticipation(participantId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/participants/{participantId}/decline")
    public ResponseEntity<Void> declineParticipation(@PathVariable UUID participantId) {
        eventService.declineParticipation(participantId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{eventId}/songs")
    @PreAuthorize("@projectSecurity.isMemberByEventId(#eventId)")
    public ResponseEntity<Void> addSetListItem(@PathVariable UUID eventId, @RequestBody @Valid List<AddEventSetlistItemDTO> addEventSetlistItemDto) {
        eventService.addSetListItemToEvent(eventId, addEventSetlistItemDto);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{eventId}/setlist/{setlistItemId}")
    @PreAuthorize("@projectSecurity.isMemberByEventId(#eventId)")
    public ResponseEntity<Void> deleteSetlistItemFromEvent(@PathVariable UUID eventId, @PathVariable UUID setlistItemId) {
        eventService.removeSetlistItemFromEvent(eventId, setlistItemId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}/setlist")
    @PreAuthorize("@projectSecurity.isMemberByEventId(#eventId)")
    public ResponseEntity<List<SetlistDTO>> getSetlist(@PathVariable UUID eventId) {
        List<SetlistDTO> setlist = eventService.getSetlist(eventId);
        return ResponseEntity.ok(setlist);
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/br/com/louvor4/api/controllers/EventController.java
git commit -m "feat: aplica @PreAuthorize de membership em EventController"
```

---

### Task 6: Aplicar `@PreAuthorize` no `EventProgramController`

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/controllers/EventProgramController.java`

Todos os endpoints de programa de evento ficam em `/events/{eventId}/program`. Leitura → `isMemberByEventId`; escrita → `isAdminOrOwnerByEventId`.

- [ ] **Step 1: Substituir o conteúdo completo do controller**

```java
package br.com.louvor4.api.controllers;

import br.com.louvor4.api.services.ProgramService;
import br.com.louvor4.api.shared.dto.Program.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/events/{eventId}/program")
public class EventProgramController {

    private final ProgramService programService;

    public EventProgramController(ProgramService programService) {
        this.programService = programService;
    }

    @GetMapping
    @PreAuthorize("@projectSecurity.isMemberByEventId(#eventId)")
    public ResponseEntity<List<ProgramItemResponse>> getProgram(@PathVariable UUID eventId) {
        return ResponseEntity.ok(programService.getProgram(eventId));
    }

    @PostMapping("/text")
    @PreAuthorize("@projectSecurity.isAdminOrOwnerByEventId(#eventId)")
    public ResponseEntity<Void> addTextItem(
            @PathVariable UUID eventId,
            @RequestBody @Valid CreateTextProgramItemRequest request) {
        programService.addTextItem(eventId, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/{itemId}")
    @PreAuthorize("@projectSecurity.isAdminOrOwnerByEventId(#eventId)")
    public ResponseEntity<Void> updateTextItem(
            @PathVariable UUID eventId,
            @PathVariable UUID itemId,
            @RequestBody @Valid UpdateTextProgramItemRequest request) {
        programService.updateTextItem(eventId, itemId, request);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{itemId}")
    @PreAuthorize("@projectSecurity.isAdminOrOwnerByEventId(#eventId)")
    public ResponseEntity<Void> deleteTextItem(
            @PathVariable UUID eventId,
            @PathVariable UUID itemId) {
        programService.deleteTextItem(eventId, itemId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/reorder")
    @PreAuthorize("@projectSecurity.isAdminOrOwnerByEventId(#eventId)")
    public ResponseEntity<Void> reorder(
            @PathVariable UUID eventId,
            @RequestBody @Valid ReorderProgramRequest request) {
        programService.reorder(eventId, request);
        return ResponseEntity.noContent().build();
    }
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/br/com/louvor4/api/controllers/EventProgramController.java
git commit -m "feat: aplica @PreAuthorize de membership em EventProgramController"
```

---

### Task 7: Verificação final — compilação e todos os testes unitários

- [ ] **Step 1: Compilar**

```bash
cd /home/erivaldo/repositorio/erivaldo/louvor4-api && ./mvnw compile -q 2>&1 | tail -10
```

Saída esperada: `BUILD SUCCESS`.

- [ ] **Step 2: Rodar todos os testes unitários (excluindo contextLoads que requer AWS)**

```bash
cd /home/erivaldo/repositorio/erivaldo/louvor4-api && ./mvnw test -Dtest='!Louvor4ApiApplicationTests' -q 2>&1 | tail -15
```

Saída esperada: `BUILD SUCCESS` com todos os testes passando (mínimo 31 = 22 existentes + 9 novos).

- [ ] **Step 3: Commit final se houver ajustes**

```bash
git add -A
git commit -m "chore: ajustes pós-compilação na segurança por projeto"
```

---

## Resumo de permissões aplicadas

| Endpoint | Proteção |
|---|---|
| `GET /music-project/{id}` | `isMember` |
| `PUT /music-project/{id}` | `isAdminOrOwner` |
| `PUT /music-project/{id}/profile-image` | `isAdminOrOwner` |
| `POST /music-project/{projectId}/members` | `isAdminOrOwner` |
| `GET /music-project/{projectId}/members` | `isMember` |
| `GET /music-project/{projectId}/members/{memberId}` | `isMember` |
| `PUT /music-project/{projectId}/members/{memberId}` | `isAdminOrOwner` |
| `DELETE /music-project/{projectId}/members/{memberId}` | `isAdminOrOwner` |
| `POST /music-project/{projectId}/events` | `isAdminOrOwner` |
| `GET /music-project/{projectId}/events` | `isMember` |
| `POST /music-project/{projectId}/skills` | `isAdminOrOwner` |
| `GET /music-project/{projectId}/skills` | `isMember` |
| `POST /music-project/{projectId}/members/{memberId}/skills` | `isAdminOrOwner` |
| `GET /music-project/{projectId}/member-role` | `isMember` |
| `GET /music-project/{projectId}/months/{ym}/overview` | `isMember` |
| `GET /events/{id}` | `isMemberByEventId` |
| `DELETE /events/{id}` | `isAdminOrOwnerByEventId` |
| `PUT /events/{eventId}` | `isAdminOrOwnerByEventId` |
| `GET /events/{eventId}/participants` | `isMemberByEventId` |
| `POST /events/{eventId}/participants` | `isAdminOrOwnerByEventId` |
| `POST /events/{eventId}/songs` | `isMemberByEventId` |
| `DELETE /events/{eventId}/setlist/{itemId}` | `isMemberByEventId` |
| `GET /events/{eventId}/setlist` | `isMemberByEventId` |
| `GET /events/{eventId}/program` | `isMemberByEventId` |
| `POST /events/{eventId}/program/text` | `isAdminOrOwnerByEventId` |
| `PUT /events/{eventId}/program/{itemId}` | `isAdminOrOwnerByEventId` |
| `DELETE /events/{eventId}/program/{itemId}` | `isAdminOrOwnerByEventId` |
| `PATCH /events/{eventId}/program/reorder` | `isAdminOrOwnerByEventId` |
