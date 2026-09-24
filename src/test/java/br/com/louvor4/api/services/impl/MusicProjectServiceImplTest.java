package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.NotificationType;
import br.com.louvor4.api.enums.ProjectMemberRole;
import br.com.louvor4.api.enums.ProjectMemberStatus;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.mapper.*;
import br.com.louvor4.api.models.MusicProject;
import br.com.louvor4.api.models.MusicProjectMember;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.*;
import br.com.louvor4.api.services.StorageService;
import br.com.louvor4.api.services.UserNotificationService;
import br.com.louvor4.api.services.UserService;
import br.com.louvor4.api.shared.dto.MusicProject.AddMemberDTO;
import br.com.louvor4.api.shared.dto.MusicProject.ProjectInviteDTO;
import br.com.louvor4.api.shared.dto.MusicProject.ProjectInviteResponseDTO;
import br.com.louvor4.api.shared.dto.notification.CreateUserNotificationRequest;
import br.com.louvor4.entitlement.exceptions.PlanLimitExceededException;
import br.com.louvor4.entitlement.services.EntitlementService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MusicProjectServiceImplTest {

    @Mock MusicProjectRepository musicProjectRepository;
    @Mock MusicProjectMemberRepository musicProjectMemberRepository;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock StorageService storageService;
    @Mock UserService userService;
    @Mock UserNotificationService userNotificationService;
    @Mock MusicProjectMemberMapper musicProjectMemberMapper;
    @Mock EventMapper eventMapper;
    @Mock EventSetlistItemMapper eventSetlistItemMapper;
    @Mock EventParticipantMapper eventParticipantMapper;
    @Mock EventOverviewMapper eventOverviewMapper;
    @Mock EventRepository eventRepository;
    @Mock ProjectSkillRepository projectSkillRepository;
    @Mock MemberMapper memberMapper;
    @Mock EventParticipantRepository eventParticipantRepository;
    @Mock EventSetlistItemRepository eventSetlistItemRepository;
    @Mock EntitlementService entitlementService;

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

        project = new MusicProject();
        project.setId(projectId);
        project.setName("Louvor da Manhã");
    }

    @Test
    void addMemberShouldCreatePendingInviteAndSendNotification() {
        AddMemberDTO dto = new AddMemberDTO();
        dto.setUsername("joao");

        when(userService.findByUsername("joao")).thenReturn(invitedUser);
        when(musicProjectMemberRepository.existsByMusicProject_IdAndUser_IdAndStatus(projectId, invitedUserId, ProjectMemberStatus.ACTIVE)).thenReturn(false);
        when(musicProjectMemberRepository.existsByMusicProject_IdAndUser_IdAndStatus(projectId, invitedUserId, ProjectMemberStatus.PENDING_INVITE)).thenReturn(false);
        when(currentUserProvider.get()).thenReturn(admin);
        stubMemberLimit(admin, 10, 3);
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

    // --- limite max_project_members (plano do dono do projeto) ---

    private void stubMemberLimit(User owner, int limit, long currentMembers) {
        MusicProjectMember ownerMember = new MusicProjectMember();
        ownerMember.setUser(owner);
        ownerMember.setProjectRole(ProjectMemberRole.OWNER);
        when(musicProjectMemberRepository.findFirstByMusicProject_IdAndProjectRoleAndStatus(
                projectId, ProjectMemberRole.OWNER, ProjectMemberStatus.ACTIVE)).thenReturn(Optional.of(ownerMember));
        when(entitlementService.getLimit(owner.getId(), "max_project_members")).thenReturn(limit);
        lenient().when(musicProjectMemberRepository.countByMusicProject_IdAndStatusIn(
                projectId, List.of(ProjectMemberStatus.ACTIVE, ProjectMemberStatus.PENDING_INVITE))).thenReturn(currentMembers);
    }

    private AddMemberDTO inviteJoao() {
        AddMemberDTO dto = new AddMemberDTO();
        dto.setUsername("joao");
        when(userService.findByUsername("joao")).thenReturn(invitedUser);
        when(musicProjectMemberRepository.existsByMusicProject_IdAndUser_IdAndStatus(projectId, invitedUserId, ProjectMemberStatus.ACTIVE)).thenReturn(false);
        when(musicProjectMemberRepository.existsByMusicProject_IdAndUser_IdAndStatus(projectId, invitedUserId, ProjectMemberStatus.PENDING_INVITE)).thenReturn(false);
        return dto;
    }

    @Test
    void addMemberShouldBlockWhenActivePlusPendingReachOwnerLimit() {
        AddMemberDTO dto = inviteJoao();
        when(currentUserProvider.get()).thenReturn(admin);
        stubMemberLimit(admin, 10, 10);

        assertThatThrownBy(() -> service.addMember(projectId, dto))
                .isInstanceOf(PlanLimitExceededException.class)
                .hasMessageContaining("máximo: 10");

        verify(musicProjectMemberRepository, never()).save(any());
        verify(userNotificationService, never()).createNotification(any());
    }

    @Test
    void addMemberShouldUseOwnerPlanAndExplainItWhenInviterIsNotTheOwner() {
        User owner = new User();
        owner.setId(UUID.randomUUID());
        AddMemberDTO dto = inviteJoao();
        when(currentUserProvider.get()).thenReturn(admin);
        stubMemberLimit(owner, 5, 5);

        assertThatThrownBy(() -> service.addMember(projectId, dto))
                .isInstanceOf(PlanLimitExceededException.class)
                .hasMessage("Este projeto atingiu o limite de membros do plano do proprietário (máximo: 5).");

        verify(entitlementService, never()).getLimit(eq(adminId), any());
    }

    @Test
    void addMemberShouldAllowUnlimitedPlan() {
        AddMemberDTO dto = inviteJoao();
        when(currentUserProvider.get()).thenReturn(admin);
        stubMemberLimit(admin, -1, 500);
        when(userService.findUserById(invitedUserId)).thenReturn(invitedUser);
        when(musicProjectRepository.getMusicProjectById(projectId)).thenReturn(project);
        when(musicProjectMemberRepository.findByMusicProject_IdAndUser_IdAndStatus(projectId, invitedUserId, ProjectMemberStatus.DECLINED)).thenReturn(Optional.empty());
        MusicProjectMember savedMember = new MusicProjectMember();
        savedMember.setId(UUID.randomUUID());
        when(musicProjectMemberRepository.save(any())).thenReturn(savedMember);

        service.addMember(projectId, dto);

        verify(musicProjectMemberRepository).save(any());
    }

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

    @Test
    void respondInviteAcceptedShouldActivateMemberAndNotifyAdmin() {
        when(currentUserProvider.get()).thenReturn(admin);

        MusicProjectMember pendingMember = new MusicProjectMember();
        pendingMember.setId(UUID.randomUUID());
        pendingMember.setUser(admin);
        pendingMember.setMusicProject(project);
        pendingMember.setAddedByUserId(adminId);
        pendingMember.setStatus(ProjectMemberStatus.PENDING_INVITE);

        when(musicProjectMemberRepository.findByMusicProject_IdAndUser_IdAndStatus(
                projectId, adminId, ProjectMemberStatus.PENDING_INVITE))
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
        when(currentUserProvider.get()).thenReturn(admin);

        MusicProjectMember pendingMember = new MusicProjectMember();
        pendingMember.setId(UUID.randomUUID());
        pendingMember.setUser(admin);
        pendingMember.setMusicProject(project);
        pendingMember.setAddedByUserId(adminId);
        pendingMember.setStatus(ProjectMemberStatus.PENDING_INVITE);

        when(musicProjectMemberRepository.findByMusicProject_IdAndUser_IdAndStatus(
                projectId, adminId, ProjectMemberStatus.PENDING_INVITE))
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
