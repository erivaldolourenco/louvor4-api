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
