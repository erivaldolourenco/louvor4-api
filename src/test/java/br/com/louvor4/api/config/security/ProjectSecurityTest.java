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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
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
