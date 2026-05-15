# Project Member Invite Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Alterar o fluxo de adição de membros a projetos musicais para exigir que o usuário aceite ou recuse o convite antes de entrar no projeto.

**Architecture:** O `MusicProjectMember` já possui um campo `status` com o enum `ProjectMemberStatus`; basta acrescentar os valores `PENDING_INVITE` e `DECLINED`, adicionar dois campos de timestamp (`invited_at`, `responded_at`) e ajustar o serviço para criar membros pendentes, disparar notificação e expor dois novos endpoints de resposta e listagem de convites.

**Tech Stack:** Java 21, Spring Boot 3, Spring Data JPA, PostgreSQL, JUnit 5, Mockito

---

## Mapa de arquivos

| Ação | Arquivo |
|---|---|
| Modify | `src/main/java/br/com/louvor4/api/enums/ProjectMemberStatus.java` |
| Modify | `src/main/java/br/com/louvor4/api/enums/NotificationType.java` |
| Modify | `src/main/java/br/com/louvor4/api/models/MusicProjectMember.java` |
| Modify | `src/main/java/br/com/louvor4/api/repositories/MusicProjectMemberRepository.java` |
| Create | `src/main/java/br/com/louvor4/api/shared/dto/MusicProject/ProjectInviteResponseDTO.java` |
| Create | `src/main/java/br/com/louvor4/api/shared/dto/MusicProject/ProjectInviteDTO.java` |
| Modify | `src/main/java/br/com/louvor4/api/services/MusicProjectService.java` |
| Modify | `src/main/java/br/com/louvor4/api/services/impl/MusicProjectServiceImpl.java` |
| Modify | `src/main/java/br/com/louvor4/api/controllers/MusicProjectController.java` |
| Create | `src/test/java/br/com/louvor4/api/services/impl/MusicProjectServiceImplTest.java` |

---

### Task 1: Adicionar status ao enum `ProjectMemberStatus`

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/enums/ProjectMemberStatus.java`

- [ ] **Step 1: Adicionar os valores `PENDING_INVITE` e `DECLINED`**

```java
package br.com.louvor4.api.enums;

public enum ProjectMemberStatus {
    ACTIVE,
    REMOVED,
    PENDING_INVITE,
    DECLINED
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/br/com/louvor4/api/enums/ProjectMemberStatus.java
git commit -m "feat: adiciona status PENDING_INVITE e DECLINED ao ProjectMemberStatus"
```

---

### Task 2: Adicionar tipos de notificação de convite de projeto

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/enums/NotificationType.java`

- [ ] **Step 1: Adicionar três novos valores ao enum**

```java
package br.com.louvor4.api.enums;

public enum NotificationType {
    EVENT_INVITE,
    EVENT_PARTICIPANT_ACCEPTED,
    EVENT_PARTICIPANT_DECLINED,
    EVENT_PARTICIPANT_REMOVED,
    EVENT_UPDATED,
    EVENT_CANCELLED,
    EVENT_REMINDER,

    EVENT_SONG_ADDED,
    EVENT_SONG_REMOVED,
    EVENT_PROGRAM_UPDATED,

    PROJECT_MEMBER_ADDED,
    PROJECT_MEMBER_REMOVED,
    PROJECT_MEMBER_INVITE,
    PROJECT_MEMBER_INVITE_ACCEPTED,
    PROJECT_MEMBER_INVITE_DECLINED,

    MESSAGE_RECEIVED,

    SYSTEM_NOTIFICATION
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/br/com/louvor4/api/enums/NotificationType.java
git commit -m "feat: adiciona tipos de notificação para convite de membro de projeto"
```

---

### Task 3: Adicionar campos `invited_at` e `responded_at` em `MusicProjectMember`

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/models/MusicProjectMember.java`

- [ ] **Step 1: Adicionar os dois campos e seus getters/setters**

Após o campo `createdAt` existente (linha ~59), adicionar:

```java
    @Column(name = "invited_at")
    private LocalDateTime invitedAt;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;
```

E os getters/setters correspondentes ao final da classe, antes do `}` de fechamento:

```java
    public LocalDateTime getInvitedAt() {
        return invitedAt;
    }

    public void setInvitedAt(LocalDateTime invitedAt) {
        this.invitedAt = invitedAt;
    }

    public LocalDateTime getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(LocalDateTime respondedAt) {
        this.respondedAt = respondedAt;
    }
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/br/com/louvor4/api/models/MusicProjectMember.java
git commit -m "feat: adiciona campos invited_at e responded_at ao MusicProjectMember"
```

---

### Task 4: Adicionar queries ao `MusicProjectMemberRepository`

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/repositories/MusicProjectMemberRepository.java`

- [ ] **Step 1: Adicionar queries necessárias para convites**

```java
package br.com.louvor4.api.repositories;

import br.com.louvor4.api.enums.ProjectMemberStatus;
import br.com.louvor4.api.models.MusicProjectMember;
import br.com.louvor4.api.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MusicProjectMemberRepository extends JpaRepository<MusicProjectMember, UUID> {
    boolean existsByMusicProject_IdAndUser_Id(UUID projectId, UUID userId);
    List<MusicProjectMember> getMusicProjectMembersByUser_Id(UUID userId);
    List<MusicProjectMember> getMusicProjectMembersByMusicProject_Id(UUID projectId);
    UUID user(User user);
    Optional<MusicProjectMember> findByMusicProject_IdAndUser_Id(UUID projectId, UUID userId);
    Optional<MusicProjectMember> findById(UUID memberId);
    long countByUser_IdAndProjectRole(UUID userId, br.com.louvor4.api.enums.ProjectMemberRole projectRole);

    // novas queries para convite
    boolean existsByMusicProject_IdAndUser_IdAndStatus(UUID projectId, UUID userId, ProjectMemberStatus status);
    List<MusicProjectMember> findByUser_IdAndStatus(UUID userId, ProjectMemberStatus status);
    Optional<MusicProjectMember> findByMusicProject_IdAndUser_IdAndStatus(UUID projectId, UUID userId, ProjectMemberStatus status);
    List<MusicProjectMember> findByMusicProject_IdAndStatus(UUID projectId, ProjectMemberStatus status);
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/br/com/louvor4/api/repositories/MusicProjectMemberRepository.java
git commit -m "feat: adiciona queries de convite ao MusicProjectMemberRepository"
```

---

### Task 5: Criar DTOs `ProjectInviteDTO` e `ProjectInviteResponseDTO`

**Files:**
- Create: `src/main/java/br/com/louvor4/api/shared/dto/MusicProject/ProjectInviteDTO.java`
- Create: `src/main/java/br/com/louvor4/api/shared/dto/MusicProject/ProjectInviteResponseDTO.java`

- [ ] **Step 1: Criar `ProjectInviteDTO` (response do GET /invites)**

```java
package br.com.louvor4.api.shared.dto.MusicProject;

import java.time.LocalDateTime;
import java.util.UUID;

public class ProjectInviteDTO {
    private UUID memberId;
    private UUID projectId;
    private String projectName;
    private String projectProfileImage;
    private UUID invitedByUserId;
    private LocalDateTime invitedAt;

    public UUID getMemberId() { return memberId; }
    public void setMemberId(UUID memberId) { this.memberId = memberId; }

    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getProjectProfileImage() { return projectProfileImage; }
    public void setProjectProfileImage(String projectProfileImage) { this.projectProfileImage = projectProfileImage; }

    public UUID getInvitedByUserId() { return invitedByUserId; }
    public void setInvitedByUserId(UUID invitedByUserId) { this.invitedByUserId = invitedByUserId; }

    public LocalDateTime getInvitedAt() { return invitedAt; }
    public void setInvitedAt(LocalDateTime invitedAt) { this.invitedAt = invitedAt; }
}
```

- [ ] **Step 2: Criar `ProjectInviteResponseDTO` (request do POST /invite/respond)**

```java
package br.com.louvor4.api.shared.dto.MusicProject;

import jakarta.validation.constraints.NotNull;

public class ProjectInviteResponseDTO {
    @NotNull
    private Boolean accepted;

    public Boolean getAccepted() { return accepted; }
    public void setAccepted(Boolean accepted) { this.accepted = accepted; }
}
```

- [ ] **Step 3: Commit**

```bash
git add src/main/java/br/com/louvor4/api/shared/dto/MusicProject/ProjectInviteDTO.java
git add src/main/java/br/com/louvor4/api/shared/dto/MusicProject/ProjectInviteResponseDTO.java
git commit -m "feat: adiciona DTOs ProjectInviteDTO e ProjectInviteResponseDTO"
```

---

### Task 6: Adicionar métodos à interface `MusicProjectService`

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/services/MusicProjectService.java`

- [ ] **Step 1: Adicionar as assinaturas dos dois novos métodos**

```java
package br.com.louvor4.api.services;

import br.com.louvor4.api.enums.ProjectMemberRole;
import br.com.louvor4.api.shared.dto.Event.CreateEventDto;
import br.com.louvor4.api.shared.dto.Event.EventDetailDto;
import br.com.louvor4.api.shared.dto.MusicProject.*;
import br.com.louvor4.api.shared.dto.eventOverview.MonthOverviewResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface MusicProjectService {
    MusicProjectDetailDTO create(MusicProjectCreateDTO musicProjectCreateDTO);
    MusicProjectDetailDTO update(UUID id, MusicProjectDTO updateDTO);
    String updateImage(UUID projecId, MultipartFile profileImage);
    MusicProjectDetailDTO getById(UUID projectId);
    List<MusicProjectDTO> getFromUser();

    void addMember(UUID projectId, AddMemberDTO addDto);
    List<MemberDTO> getMembers(UUID projectId);

    List<ProjectInviteDTO> getMyInvites();
    void respondInvite(UUID projectId, ProjectInviteResponseDTO responseDto);

    CreateEventDto createEvent(UUID projectId, CreateEventDto eventDto);
    List<EventDetailDto> getEventsByProject(UUID projectId);

    void assignSkillsToMember(UUID projectId, UUID memberId, List<UUID> skillIds);
    void addProjectSkill(UUID projectId, ProjectSkillRequestDTO skillDto);

    List<ProjectSkillDTO> getProjectSkills(UUID projectId);

    MemberDTO getMember(UUID projectId, UUID memberId);

    MemberDTO updateMember(UUID projectId, UUID memberId, UpdateMemberRequest request);

    void deleteMember(UUID projectId, UUID memberId);

    ProjectMemberRole getMemberRole(UUID projectId);

    MonthOverviewResponse getMonthOverview(UUID projectId, String yearMonth);
}
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/br/com/louvor4/api/services/MusicProjectService.java
git commit -m "feat: adiciona getMyInvites e respondInvite à interface MusicProjectService"
```

---

### Task 7: Implementar a lógica de convite em `MusicProjectServiceImpl`

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/services/impl/MusicProjectServiceImpl.java`

Esta é a tarefa central. Envolve quatro mudanças no impl:

**A) Injetar `UserNotificationService`** no construtor.

**B) Modificar `addMember`** para criar o membro com `PENDING_INVITE` e enviar notificação.

**C) Modificar `validIfMemberExistInProject`** para distinguir entre membro ativo e convite pendente.

**D) Implementar `getMyInvites` e `respondInvite`.**

**E) Filtrar apenas `ACTIVE` em `getFromUser` e `getMembers`.**

- [ ] **Step 1: Adicionar `UserNotificationService` como dependência**

No topo da classe, adicionar o campo:
```java
    private final UserNotificationService userNotificationService;
```

No construtor (que já tem muitos parâmetros), adicionar `UserNotificationService userNotificationService` como último parâmetro e `this.userNotificationService = userNotificationService;` no corpo. Exemplo de como o construtor deve ficar — adicione apenas o parâmetro e a atribuição, não reescreva os existentes:

```java
    public MusicProjectServiceImpl(..., UserNotificationService userNotificationService) {
        // ... atribuições existentes ...
        this.userNotificationService = userNotificationService;
    }
```

Adicionar o import:
```java
import br.com.louvor4.api.services.UserNotificationService;
```

- [ ] **Step 2: Substituir o método `addMember` completo**

```java
    @Override
    @Transactional
    public void addMember(UUID projectId, AddMemberDTO addDto) {
        User user = userService.findByUsername(addDto.getUsername());

        boolean isActive = musicProjectMemberRepository
                .existsByMusicProject_IdAndUser_IdAndStatus(projectId, user.getId(), ProjectMemberStatus.ACTIVE);
        if (isActive) {
            throw new ValidationException("Usuário já é membro deste projeto.");
        }

        boolean hasPendingInvite = musicProjectMemberRepository
                .existsByMusicProject_IdAndUser_IdAndStatus(projectId, user.getId(), ProjectMemberStatus.PENDING_INVITE);
        if (hasPendingInvite) {
            throw new ValidationException("Já existe um convite pendente para este usuário.");
        }

        User creator = currentUserProvider.get();
        User userMember = userService.findUserById(user.getId());
        MusicProject musicProject = musicProjectRepository.getMusicProjectById(projectId);

        Optional<MusicProjectMember> declinedOpt = musicProjectMemberRepository
                .findByMusicProject_IdAndUser_IdAndStatus(projectId, user.getId(), ProjectMemberStatus.DECLINED);

        MusicProjectMember musicProjectMember = declinedOpt.orElseGet(MusicProjectMember::new);
        musicProjectMember.setUser(userMember);
        musicProjectMember.setMusicProject(musicProject);
        musicProjectMember.setAddedByUserId(creator.getId());
        musicProjectMember.setProjectRole(ProjectMemberRole.MEMBER);
        musicProjectMember.setStatus(ProjectMemberStatus.PENDING_INVITE);
        musicProjectMember.setInvitedAt(LocalDateTime.now());
        musicProjectMember.setRespondedAt(null);

        musicProjectMemberRepository.save(musicProjectMember);

        String dataJson = String.format(
                "{\"projectId\":\"%s\",\"projectName\":\"%s\",\"invitedByUserId\":\"%s\",\"memberId\":\"%s\"}",
                musicProject.getId(), musicProject.getName(), creator.getId(), musicProjectMember.getId()
        );

        userNotificationService.createNotification(new CreateUserNotificationRequest(
                NotificationType.PROJECT_MEMBER_INVITE,
                userMember.getId(),
                "Convite para projeto",
                "Você foi convidado para participar do projeto " + musicProject.getName() + ". Aceite ou recuse o convite.",
                null,
                dataJson
        ));
    }
```

Adicionar os imports necessários (se ainda não existirem):
```java
import br.com.louvor4.api.enums.ProjectMemberStatus;
import br.com.louvor4.api.shared.dto.notification.CreateUserNotificationRequest;
import br.com.louvor4.api.enums.NotificationType;
import br.com.louvor4.api.shared.dto.MusicProject.ProjectInviteDTO;
import br.com.louvor4.api.shared.dto.MusicProject.ProjectInviteResponseDTO;
```

- [ ] **Step 3: Remover o método `validIfMemberExistInProject`** (não é mais chamado; a lógica agora está dentro de `addMember`).

- [ ] **Step 4: Implementar `getMyInvites`**

Adicionar o método na impl:

```java
    @Override
    @Transactional(readOnly = true)
    public List<ProjectInviteDTO> getMyInvites() {
        UUID userId = currentUserProvider.get().getId();
        return musicProjectMemberRepository
                .findByUser_IdAndStatus(userId, ProjectMemberStatus.PENDING_INVITE)
                .stream()
                .map(member -> {
                    MusicProject project = member.getMusicProject();
                    ProjectInviteDTO dto = new ProjectInviteDTO();
                    dto.setMemberId(member.getId());
                    dto.setProjectId(project.getId());
                    dto.setProjectName(project.getName());
                    dto.setProjectProfileImage(project.getProfileImage());
                    dto.setInvitedByUserId(member.getAddedByUserId());
                    dto.setInvitedAt(member.getInvitedAt());
                    return dto;
                })
                .toList();
    }
```

- [ ] **Step 5: Implementar `respondInvite`**

```java
    @Override
    @Transactional
    public void respondInvite(UUID projectId, ProjectInviteResponseDTO responseDto) {
        User currentUser = currentUserProvider.get();

        MusicProjectMember member = musicProjectMemberRepository
                .findByMusicProject_IdAndUser_IdAndStatus(projectId, currentUser.getId(), ProjectMemberStatus.PENDING_INVITE)
                .orElseThrow(() -> new ValidationException("Convite não encontrado para este projeto."));

        member.setRespondedAt(LocalDateTime.now());

        if (Boolean.TRUE.equals(responseDto.getAccepted())) {
            member.setStatus(ProjectMemberStatus.ACTIVE);
            musicProjectMemberRepository.save(member);

            MusicProject project = member.getMusicProject();
            String dataJson = String.format(
                    "{\"projectId\":\"%s\",\"projectName\":\"%s\",\"acceptedByUserId\":\"%s\"}",
                    project.getId(), project.getName(), currentUser.getId()
            );
            if (member.getAddedByUserId() != null) {
                userNotificationService.createNotification(new CreateUserNotificationRequest(
                        NotificationType.PROJECT_MEMBER_INVITE_ACCEPTED,
                        member.getAddedByUserId(),
                        "Convite aceito",
                        currentUser.getFirstName() + " aceitou o convite para o projeto " + project.getName() + ".",
                        null,
                        dataJson
                ));
            }
        } else {
            member.setStatus(ProjectMemberStatus.DECLINED);
            musicProjectMemberRepository.save(member);

            MusicProject project = member.getMusicProject();
            String dataJson = String.format(
                    "{\"projectId\":\"%s\",\"projectName\":\"%s\",\"declinedByUserId\":\"%s\"}",
                    project.getId(), project.getName(), currentUser.getId()
            );
            if (member.getAddedByUserId() != null) {
                userNotificationService.createNotification(new CreateUserNotificationRequest(
                        NotificationType.PROJECT_MEMBER_INVITE_DECLINED,
                        member.getAddedByUserId(),
                        "Convite recusado",
                        currentUser.getFirstName() + " recusou o convite para o projeto " + project.getName() + ".",
                        null,
                        dataJson
                ));
            }
        }
    }
```

- [ ] **Step 6: Filtrar apenas `ACTIVE` em `getFromUser`**

Localizar o método `getFromUser` (linha ~152). Ele chama `getMusicProjectMembersByUser_Id`. Substituir por query filtrada por status:

```java
    @Override
    public List<MusicProjectDTO> getFromUser() {
        UUID userId = currentUserProvider.get().getId();
        List<MusicProjectMember> musicProjectMember = musicProjectMemberRepository
                .findByUser_IdAndStatus(userId, ProjectMemberStatus.ACTIVE);

        return musicProjectMember.stream()
                .map(MusicProjectMember::getMusicProject)
                .distinct()
                .map(project -> {
                    MusicProjectDTO dto = new MusicProjectDTO();
                    dto.setId(project.getId());
                    dto.setName(project.getName());
                    dto.setType(project.getType());
                    dto.setProfileImage(project.getProfileImage());
                    dto.setProfileImageHash(project.getProfileImageHash());
                    return dto;
                })
                .toList();
    }
```

- [ ] **Step 7: Filtrar apenas `ACTIVE` em `getMembers`**

Localizar `getMembers` (linha ~191). Substituir a query para retornar só membros ativos:

```java
    @Override
    public List<MemberDTO> getMembers(UUID projectId) {
        List<MusicProjectMember> musicProjectMembers = musicProjectMemberRepository
                .findByMusicProject_IdAndStatus(projectId, ProjectMemberStatus.ACTIVE);
        return musicProjectMemberMapper.toDtoList(musicProjectMembers);
    }
```

- [ ] **Step 8: Commit**

```bash
git add src/main/java/br/com/louvor4/api/services/impl/MusicProjectServiceImpl.java
git commit -m "feat: implementa fluxo de convite de membro em MusicProjectServiceImpl"
```

---

### Task 8: Expor os novos endpoints em `MusicProjectController`

**Files:**
- Modify: `src/main/java/br/com/louvor4/api/controllers/MusicProjectController.java`

- [ ] **Step 1: Adicionar os dois novos endpoints**

Após o endpoint `addMember` existente (linha ~62), adicionar:

```java
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
```

Adicionar imports necessários:
```java
import br.com.louvor4.api.shared.dto.MusicProject.ProjectInviteDTO;
import br.com.louvor4.api.shared.dto.MusicProject.ProjectInviteResponseDTO;
```

- [ ] **Step 2: Commit**

```bash
git add src/main/java/br/com/louvor4/api/controllers/MusicProjectController.java
git commit -m "feat: expõe endpoints GET /invites e POST /{projectId}/members/invite/respond"
```

---

### Task 9: Testes unitários para `MusicProjectServiceImpl`

**Files:**
- Create: `src/test/java/br/com/louvor4/api/services/impl/MusicProjectServiceImplTest.java`

- [ ] **Step 1: Escrever os testes**

```java
package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.NotificationType;
import br.com.louvor4.api.enums.ProjectMemberRole;
import br.com.louvor4.api.enums.ProjectMemberStatus;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.models.MusicProject;
import br.com.louvor4.api.models.MusicProjectMember;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.MusicProjectMemberRepository;
import br.com.louvor4.api.repositories.MusicProjectRepository;
import br.com.louvor4.api.services.UserNotificationService;
import br.com.louvor4.api.services.UserService;
import br.com.louvor4.api.shared.dto.MusicProject.AddMemberDTO;
import br.com.louvor4.api.shared.dto.MusicProject.ProjectInviteDTO;
import br.com.louvor4.api.shared.dto.MusicProject.ProjectInviteResponseDTO;
import br.com.louvor4.api.shared.dto.notification.CreateUserNotificationRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MusicProjectServiceImplTest {

    @Mock MusicProjectRepository musicProjectRepository;
    @Mock MusicProjectMemberRepository musicProjectMemberRepository;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock UserService userService;
    @Mock UserNotificationService userNotificationService;

    @InjectMocks MusicProjectServiceImpl service;

    private UUID projectId;
    private UUID invitedUserId;
    private UUID adminId;
    private User admin;
    private User invitedUser;
    private MusicProject project;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        invitedUserId = UUID.randomUUID();
        adminId = UUID.randomUUID();

        admin = new User();
        admin.setId(adminId);
        admin.setFirstName("Admin");

        invitedUser = new User();
        invitedUser.setId(invitedUserId);
        invitedUser.setUsername("joao");

        project = new MusicProject();
        project.setId(projectId);
        project.setName("Louvor da Manhã");
    }

    // --- addMember ---

    @Test
    void addMemberShouldCreatePendingInviteAndSendNotification() {
        AddMemberDTO dto = new AddMemberDTO();
        dto.setUsername("joao");

        when(userService.findByUsername("joao")).thenReturn(invitedUser);
        when(musicProjectMemberRepository.existsByMusicProject_IdAndUser_IdAndStatus(projectId, invitedUserId, ProjectMemberStatus.ACTIVE)).thenReturn(false);
        when(musicProjectMemberRepository.existsByMusicProject_IdAndUser_IdAndStatus(projectId, invitedUserId, ProjectMemberStatus.PENDING_INVITE)).thenReturn(false);
        when(currentUserProvider.get()).thenReturn(admin);
        when(userService.findUserById(invitedUserId)).thenReturn(invitedUser);
        when(musicProjectRepository.getMusicProjectById(projectId)).thenReturn(project);
        when(musicProjectMemberRepository.findByMusicProject_IdAndUser_IdAndStatus(projectId, invitedUserId, ProjectMemberStatus.DECLINED)).thenReturn(Optional.empty());

        MusicProjectMember savedMember = new MusicProjectMember();
        savedMember.setId(UUID.randomUUID());
        when(musicProjectMemberRepository.save(any())).thenReturn(savedMember);

        service.addMember(projectId, dto);

        ArgumentCaptor<MusicProjectMember> memberCaptor = ArgumentCaptor.forClass(MusicProjectMember.class);
        verify(musicProjectMemberRepository).save(memberCaptor.capture());
        assertThat(memberCaptor.getValue().getStatus()).isEqualTo(ProjectMemberStatus.PENDING_INVITE);
        assertThat(memberCaptor.getValue().getInvitedAt()).isNotNull();

        ArgumentCaptor<CreateUserNotificationRequest> notifCaptor = ArgumentCaptor.forClass(CreateUserNotificationRequest.class);
        verify(userNotificationService).createNotification(notifCaptor.capture());
        assertThat(notifCaptor.getValue().type()).isEqualTo(NotificationType.PROJECT_MEMBER_INVITE);
        assertThat(notifCaptor.getValue().userId()).isEqualTo(invitedUserId);
    }

    @Test
    void addMemberShouldThrowWhenUserIsAlreadyActive() {
        AddMemberDTO dto = new AddMemberDTO();
        dto.setUsername("joao");

        when(userService.findByUsername("joao")).thenReturn(invitedUser);
        when(musicProjectMemberRepository.existsByMusicProject_IdAndUser_IdAndStatus(projectId, invitedUserId, ProjectMemberStatus.ACTIVE)).thenReturn(true);

        assertThatThrownBy(() -> service.addMember(projectId, dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("já é membro");
    }

    @Test
    void addMemberShouldThrowWhenPendingInviteAlreadyExists() {
        AddMemberDTO dto = new AddMemberDTO();
        dto.setUsername("joao");

        when(userService.findByUsername("joao")).thenReturn(invitedUser);
        when(musicProjectMemberRepository.existsByMusicProject_IdAndUser_IdAndStatus(projectId, invitedUserId, ProjectMemberStatus.ACTIVE)).thenReturn(false);
        when(musicProjectMemberRepository.existsByMusicProject_IdAndUser_IdAndStatus(projectId, invitedUserId, ProjectMemberStatus.PENDING_INVITE)).thenReturn(true);

        assertThatThrownBy(() -> service.addMember(projectId, dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("convite pendente");
    }

    // --- getMyInvites ---

    @Test
    void getMyInvitesShouldReturnPendingInvitesForCurrentUser() {
        when(currentUserProvider.get()).thenReturn(invitedUser);

        MusicProjectMember pendingMember = new MusicProjectMember();
        pendingMember.setId(UUID.randomUUID());
        pendingMember.setUser(invitedUser);
        pendingMember.setMusicProject(project);
        pendingMember.setAddedByUserId(adminId);
        pendingMember.setStatus(ProjectMemberStatus.PENDING_INVITE);
        pendingMember.setInvitedAt(LocalDateTime.now());

        when(musicProjectMemberRepository.findByUser_IdAndStatus(invitedUserId, ProjectMemberStatus.PENDING_INVITE))
                .thenReturn(List.of(pendingMember));

        List<ProjectInviteDTO> invites = service.getMyInvites();

        assertThat(invites).hasSize(1);
        assertThat(invites.get(0).getProjectId()).isEqualTo(projectId);
        assertThat(invites.get(0).getProjectName()).isEqualTo("Louvor da Manhã");
    }

    // --- respondInvite ---

    @Test
    void respondInviteAcceptedShouldActivateMemberAndNotifyAdmin() {
        when(currentUserProvider.get()).thenReturn(invitedUser);

        MusicProjectMember pendingMember = new MusicProjectMember();
        pendingMember.setId(UUID.randomUUID());
        pendingMember.setUser(invitedUser);
        pendingMember.setMusicProject(project);
        pendingMember.setAddedByUserId(adminId);
        pendingMember.setStatus(ProjectMemberStatus.PENDING_INVITE);

        when(musicProjectMemberRepository.findByMusicProject_IdAndUser_IdAndStatus(
                projectId, invitedUserId, ProjectMemberStatus.PENDING_INVITE))
                .thenReturn(Optional.of(pendingMember));

        ProjectInviteResponseDTO dto = new ProjectInviteResponseDTO();
        dto.setAccepted(true);

        service.respondInvite(projectId, dto);

        ArgumentCaptor<MusicProjectMember> captor = ArgumentCaptor.forClass(MusicProjectMember.class);
        verify(musicProjectMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ProjectMemberStatus.ACTIVE);
        assertThat(captor.getValue().getRespondedAt()).isNotNull();

        ArgumentCaptor<CreateUserNotificationRequest> notifCaptor = ArgumentCaptor.forClass(CreateUserNotificationRequest.class);
        verify(userNotificationService).createNotification(notifCaptor.capture());
        assertThat(notifCaptor.getValue().type()).isEqualTo(NotificationType.PROJECT_MEMBER_INVITE_ACCEPTED);
        assertThat(notifCaptor.getValue().userId()).isEqualTo(adminId);
    }

    @Test
    void respondInviteDeclinedShouldSetDeclinedStatusAndNotifyAdmin() {
        when(currentUserProvider.get()).thenReturn(invitedUser);

        MusicProjectMember pendingMember = new MusicProjectMember();
        pendingMember.setId(UUID.randomUUID());
        pendingMember.setUser(invitedUser);
        pendingMember.setMusicProject(project);
        pendingMember.setAddedByUserId(adminId);
        pendingMember.setStatus(ProjectMemberStatus.PENDING_INVITE);

        when(musicProjectMemberRepository.findByMusicProject_IdAndUser_IdAndStatus(
                projectId, invitedUserId, ProjectMemberStatus.PENDING_INVITE))
                .thenReturn(Optional.of(pendingMember));

        ProjectInviteResponseDTO dto = new ProjectInviteResponseDTO();
        dto.setAccepted(false);

        service.respondInvite(projectId, dto);

        ArgumentCaptor<MusicProjectMember> captor = ArgumentCaptor.forClass(MusicProjectMember.class);
        verify(musicProjectMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ProjectMemberStatus.DECLINED);

        ArgumentCaptor<CreateUserNotificationRequest> notifCaptor = ArgumentCaptor.forClass(CreateUserNotificationRequest.class);
        verify(userNotificationService).createNotification(notifCaptor.capture());
        assertThat(notifCaptor.getValue().type()).isEqualTo(NotificationType.PROJECT_MEMBER_INVITE_DECLINED);
    }

    @Test
    void respondInviteShouldThrowWhenNoPendingInviteExists() {
        when(currentUserProvider.get()).thenReturn(invitedUser);
        when(musicProjectMemberRepository.findByMusicProject_IdAndUser_IdAndStatus(
                projectId, invitedUserId, ProjectMemberStatus.PENDING_INVITE))
                .thenReturn(Optional.empty());

        ProjectInviteResponseDTO dto = new ProjectInviteResponseDTO();
        dto.setAccepted(true);

        assertThatThrownBy(() -> service.respondInvite(projectId, dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Convite não encontrado");
    }
}
```

- [ ] **Step 2: Rodar os testes**

```bash
./mvnw test -pl . -Dtest=MusicProjectServiceImplTest -q
```

Saída esperada: `BUILD SUCCESS` com os 7 testes passando.

- [ ] **Step 3: Commit**

```bash
git add src/test/java/br/com/louvor4/api/services/impl/MusicProjectServiceImplTest.java
git commit -m "test: adiciona testes unitários para o fluxo de convite de membro"
```

---

### Task 10: Verificação final — compilação e todos os testes

- [ ] **Step 1: Compilar o projeto**

```bash
./mvnw compile -q
```

Saída esperada: `BUILD SUCCESS` (sem erros de compilação).

- [ ] **Step 2: Rodar todos os testes**

```bash
./mvnw test -q
```

Saída esperada: `BUILD SUCCESS` com todos os testes passando.

- [ ] **Step 3: Commit final (se necessário)**

```bash
git add -A
git commit -m "chore: ajustes pós-compilação no fluxo de convite de membro"
```

---

## Notas de migração de banco

As colunas `invited_at` e `responded_at` serão `null` por padrão (Hibernate não força NOT NULL). Se o banco usa tipo enum nativo do PostgreSQL para `project_member_status`, executar antes de subir a aplicação:

```sql
ALTER TYPE project_member_status ADD VALUE IF NOT EXISTS 'PENDING_INVITE';
ALTER TYPE project_member_status ADD VALUE IF NOT EXISTS 'DECLINED';
```

Se o banco armazena o enum como `VARCHAR` (padrão do Spring/Hibernate com `@Enumerated(EnumType.STRING)`), nenhuma migração é necessária.
