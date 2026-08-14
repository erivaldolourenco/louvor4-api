package br.com.louvor4.api.config.security;

import br.com.louvor4.api.enums.EventPermission;
import br.com.louvor4.api.enums.ProjectMemberRole;
import br.com.louvor4.api.enums.ProjectMemberStatus;
import br.com.louvor4.api.repositories.EventParticipantRepository;
import br.com.louvor4.api.repositories.EventRepository;
import br.com.louvor4.api.repositories.EventSetlistItemRepository;
import br.com.louvor4.api.repositories.MedleyRepository;
import br.com.louvor4.api.repositories.MusicProjectMemberRepository;
import br.com.louvor4.api.repositories.ProjectSkillRepository;
import br.com.louvor4.api.repositories.SongCategoryRepository;
import br.com.louvor4.api.repositories.SongRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component("projectSecurity")
public class ProjectSecurity {

    private final MusicProjectMemberRepository memberRepository;
    private final CurrentUserProvider currentUserProvider;
    private final EventRepository eventRepository;
    private final SongRepository songRepository;
    private final EventSetlistItemRepository eventSetlistItemRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final MedleyRepository medleyRepository;
    private final ProjectSkillRepository projectSkillRepository;
    private final SongCategoryRepository songCategoryRepository;

    public ProjectSecurity(MusicProjectMemberRepository memberRepository,
                           CurrentUserProvider currentUserProvider,
                           EventRepository eventRepository,
                           SongRepository songRepository,
                           EventSetlistItemRepository eventSetlistItemRepository,
                           EventParticipantRepository eventParticipantRepository,
                           MedleyRepository medleyRepository,
                           ProjectSkillRepository projectSkillRepository,
                           SongCategoryRepository songCategoryRepository) {
        this.memberRepository = memberRepository;
        this.currentUserProvider = currentUserProvider;
        this.eventRepository = eventRepository;
        this.songRepository = songRepository;
        this.eventSetlistItemRepository = eventSetlistItemRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.medleyRepository = medleyRepository;
        this.projectSkillRepository = projectSkillRepository;
        this.songCategoryRepository = songCategoryRepository;
    }

    public boolean isSongCategoryOwner(UUID categoryId) {
        if (categoryId == null) return false;
        UUID userId = currentUserProvider.get().getId();
        return songCategoryRepository.existsByIdAndUser_Id(categoryId, userId);
    }

    public boolean isMember(UUID projectId) {
        UUID userId = currentUserProvider.get().getId();
        return memberRepository.existsByMusicProject_IdAndUser_IdAndStatus(
                projectId, userId, ProjectMemberStatus.ACTIVE);
    }

    public boolean isOwner(UUID projectId) {
        UUID userId = currentUserProvider.get().getId();
        return memberRepository.findByMusicProject_IdAndUser_IdAndStatus(
                        projectId, userId, ProjectMemberStatus.ACTIVE)
                .map(m -> m.getProjectRole() == ProjectMemberRole.OWNER)
                .orElse(false);
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

    public boolean isSongOwner(UUID songId) {
        if (songId == null) return false;
        UUID userId = currentUserProvider.get().getId();
        return songRepository.existsByIdAndUser_Id(songId, userId);
    }

    public boolean isMemberBySongId(UUID songId) {
        if (songId == null) return false;
        if (isSongOwner(songId)) return true;
        return eventSetlistItemRepository.findProjectIdsBySongId(songId).stream()
                .anyMatch(this::isMember);
    }

    public boolean canEditSongChordSheet(UUID songId) {
        if (songId == null) return false;
        if (isSongOwner(songId)) return true;
        if (!songRepository.existsByIdAndEditChordSheetPermissionTrue(songId)) return false;
        UUID userId = currentUserProvider.get().getId();
        return eventSetlistItemRepository.findEventIdsBySongId(songId).stream()
                .anyMatch(eventId -> hasEventPermission(eventId, userId, EventPermission.EDIT_CHORD_SHEET));
    }

    public boolean canEditEvent(UUID eventId) {
        if (isAdminOrOwnerByEventId(eventId)) return true;
        return hasEventPermission(eventId, currentUserProvider.get().getId(), EventPermission.EDIT_EVENT);
    }

    public boolean canManageParticipants(UUID eventId) {
        if (isAdminOrOwnerByEventId(eventId)) return true;
        return hasEventPermission(eventId, currentUserProvider.get().getId(), EventPermission.MANAGE_PARTICIPANTS);
    }

    private boolean hasEventPermission(UUID eventId, UUID userId, EventPermission permission) {
        return eventParticipantRepository.findByEventIdAndMemberUserId(eventId, userId)
                .map(p -> p.getPermissions().contains(permission))
                .orElse(false);
    }

    public boolean isMedleyOwner(UUID medleyId) {
        if (medleyId == null) return false;
        UUID userId = currentUserProvider.get().getId();
        return medleyRepository.existsByIdAndUser_Id(medleyId, userId);
    }

    public boolean isAdminOrOwnerBySkillId(UUID skillId) {
        if (skillId == null) return false;
        UUID projectId = projectSkillRepository.findProjectIdById(skillId);
        if (projectId == null) return false;
        return isAdminOrOwner(projectId);
    }
}
