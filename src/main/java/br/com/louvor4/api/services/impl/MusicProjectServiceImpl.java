package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.FileCategory;
import br.com.louvor4.api.enums.NotificationType;
import br.com.louvor4.api.enums.ProjectMemberRole;
import br.com.louvor4.api.enums.ProjectMemberStatus;
import br.com.louvor4.api.enums.SetlistItemType;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.mapper.*;
import br.com.louvor4.api.models.*;
import br.com.louvor4.api.repositories.*;
import br.com.louvor4.api.services.EventReminderScheduler;
import br.com.louvor4.api.services.MusicProjectService;
import br.com.louvor4.api.services.StorageService;
import br.com.louvor4.api.services.UserNotificationService;
import br.com.louvor4.api.services.UserService;
import br.com.louvor4.api.shared.dto.Event.CreateEventDto;
import br.com.louvor4.api.shared.dto.Event.EventDetailDto;
import br.com.louvor4.api.shared.dto.MusicProject.*;
import br.com.louvor4.api.shared.dto.eventOverview.MonthEventItem;
import br.com.louvor4.api.shared.dto.eventOverview.MonthOverviewResponse;
import br.com.louvor4.api.shared.dto.notification.CreateUserNotificationRequest;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.*;

import static br.com.louvor4.api.shared.util.ObjectUtils.isNotNull;
import static br.com.louvor4.api.shared.util.ObjectUtils.isNotNullOrEmpty;

@Service
public class MusicProjectServiceImpl implements MusicProjectService {
    private final MusicProjectRepository musicProjectRepository;
    private final MusicProjectMemberRepository musicProjectMemberRepository;
    private final CurrentUserProvider currentUserProvider;
    private final StorageService storageService;
    private final UserService userService;
    private final UserNotificationService userNotificationService;
    private final MusicProjectMemberMapper musicProjectMemberMapper;
    private final EventMapper eventMapper;
    private final EventSetlistItemMapper eventSetlistItemMapper;
    private final EventParticipantMapper eventParticipantMapper;
    private final EventOverviewMapper eventOverviewMapper;
    private final EventRepository eventRepository;
    private final ProjectSkillRepository projectSkillRepository;
    private final MemberMapper memberMapper;
    private final EventParticipantRepository eventParticipantRepository;
    private final EventSetlistItemRepository eventSetlistItemRepository;
    private final EventReminderScheduler eventReminderScheduler;

    public MusicProjectServiceImpl(MusicProjectRepository musicProjectRepository, MusicProjectMemberRepository musicProjectMemberRepository, CurrentUserProvider currentUserProvider, StorageService storageService, UserService userService, UserNotificationService userNotificationService, MusicProjectMemberMapper musicProjectMemberMapper, EventMapper eventMapper, EventSetlistItemMapper eventSetlistItemMapper, EventParticipantMapper eventParticipantMapper, EventOverviewMapper eventOverviewMapper, EventRepository eventRepository, ProjectSkillRepository projectSkillRepository, MemberMapper memberMapper, EventParticipantRepository eventParticipantRepository, EventSetlistItemRepository eventSetlistItemRepository, EventReminderScheduler eventReminderScheduler) {
        this.musicProjectRepository = musicProjectRepository;
        this.musicProjectMemberRepository = musicProjectMemberRepository;
        this.currentUserProvider = currentUserProvider;
        this.storageService = storageService;
        this.userService = userService;
        this.userNotificationService = userNotificationService;
        this.musicProjectMemberMapper = musicProjectMemberMapper;
        this.eventMapper = eventMapper;
        this.eventSetlistItemMapper = eventSetlistItemMapper;
        this.eventParticipantMapper = eventParticipantMapper;
        this.eventOverviewMapper = eventOverviewMapper;
        this.eventRepository = eventRepository;
        this.projectSkillRepository = projectSkillRepository;
        this.memberMapper = memberMapper;
        this.eventParticipantRepository = eventParticipantRepository;
        this.eventSetlistItemRepository = eventSetlistItemRepository;
        this.eventReminderScheduler = eventReminderScheduler;
    }

    @Override
    @Transactional
    public MusicProjectDetailDTO create(MusicProjectCreateDTO dto) {
        User creator = currentUserProvider.get();
        ensureUserCanCreateProject(creator);

        MusicProject project = buildProject(dto, creator);
        project = musicProjectRepository.save(project);

        addOwnerMember(project, creator);

        return toDetailDto(project);
    }

    private void ensureUserCanCreateProject(User user) {
        Plan plan = user.getPlan();
        int maxProjects = (plan == null || plan.getMaxProjects() == null) ? 0 : plan.getMaxProjects();
        long ownedProjects = musicProjectMemberRepository.countByUser_IdAndProjectRole(
                user.getId(), ProjectMemberRole.OWNER
        );
        if (ownedProjects >= maxProjects) {
            throw new ValidationException("Seu plano não permite criar mais projetos.");
        }
    }

    @Override
    public MusicProjectDetailDTO update(UUID projectId, MusicProjectDTO updateDTO) {
        MusicProject musicProject = musicProjectRepository.findById(projectId)
                .orElseThrow(() -> new EntityNotFoundException("MusicProject não encontrado: " + projectId));

        if (isNotNull(updateDTO.getName())) {
            musicProject.setName(updateDTO.getName().trim());
        }
        if (isNotNull(updateDTO.getType())) {
            musicProject.setType(updateDTO.getType());
        }

        MusicProject saved = musicProjectRepository.save(musicProject);

        return toDetailDto(saved);
    }

    @Override
    public String updateImage(UUID projecId, MultipartFile profileImage) {
        MusicProject musicProject = musicProjectRepository.findById(projecId)
                .orElseThrow(() -> new EntityNotFoundException("MusicProject não encontrado: " + projecId));
        if (isNotNullOrEmpty(profileImage)) {
            String fileUrl = storageService.uploadFileWithPrefix(
                    profileImage,
                    FileCategory.PROJECT_PROFILE,
                    "project-profile-" + projecId
            );
            musicProject.setProfileImage(fileUrl);
            musicProject.setProfileImageHash(extractHashFromUrl(fileUrl));
        }
        MusicProject saved = musicProjectRepository.save(musicProject);

        return saved.getProfileImage();
    }

    private String extractHashFromUrl(String url) {
        if (url == null || url.isBlank()) return null;
        int lastSlash = url.lastIndexOf('/');
        if (lastSlash < 0 || lastSlash == url.length() - 1) return null;
        String filename = url.substring(lastSlash + 1);
        int lastDash = filename.lastIndexOf('-');
        int lastDot = filename.lastIndexOf('.');
        if (lastDash <= 0) return null;
        if (lastDot > lastDash) {
            return filename.substring(lastDash + 1, lastDot);
        }
        return filename.substring(lastDash + 1);
    }

    @Override
    public MusicProjectDetailDTO getById(UUID projectId) {
        MusicProject musicProject = musicProjectRepository.getMusicProjectById(projectId);
        return toDetailDto(musicProject);
    }

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

        MusicProjectMember saved = musicProjectMemberRepository.save(musicProjectMember);

        String dataJson = String.format(
                "{\"projectId\":\"%s\",\"projectName\":\"%s\",\"invitedByUserId\":\"%s\",\"memberId\":\"%s\"}",
                musicProject.getId(), musicProject.getName(), creator.getId(), saved.getId()
        );

        userNotificationService.createNotification(new CreateUserNotificationRequest(
                NotificationType.PROJECT_MEMBER_INVITE,
                userMember.getId(),
                "Convite para projeto: " + musicProject.getName(),
                "Você foi convidado para participar do projeto " + musicProject.getName() + ". Aceite ou recuse o convite.",
                null,
                dataJson
        ));
    }

    @Override
    public List<MemberDTO> getMembers(UUID projectId) {
        List<MusicProjectMember> musicProjectMembers = musicProjectMemberRepository
                .findByMusicProject_IdAndStatus(projectId, ProjectMemberStatus.ACTIVE);
        return musicProjectMemberMapper.toDtoList(musicProjectMembers);
    }

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

    @Override
    @Transactional
    public void respondInvite(UUID projectId, ProjectInviteResponseDTO responseDto) {
        User currentUser = currentUserProvider.get();

        MusicProjectMember member = musicProjectMemberRepository
                .findByMusicProject_IdAndUser_IdAndStatus(projectId, currentUser.getId(), ProjectMemberStatus.PENDING_INVITE)
                .orElseThrow(() -> new ValidationException("Convite não encontrado para este projeto."));

        member.setRespondedAt(LocalDateTime.now());
        userNotificationService.markProjectInviteAsReadIfExists(currentUser.getId(), projectId);

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

    @Override
    public CreateEventDto createEvent(UUID projectId, CreateEventDto eventDto) {
        MusicProject project = musicProjectRepository.findById(projectId)
                .orElseThrow(() -> new ValidationException("Projeto não encontrado."));
        Event event = eventMapper.toEntity(eventDto);
        event.setMusicProject(project);
        Event saved = eventRepository.save(event);
        eventReminderScheduler.schedule(saved);
        return eventMapper.toDto(saved);
    }

    @Override
    public List<EventDetailDto> getEventsByProject(UUID projectId) {
        return eventRepository
                .findAllByMusicProject_IdAndStartAtGreaterThanEqualOrderByStartAtAsc(
                        projectId,
                        LocalDateTime.now().minusDays(1)
                )
                .stream()
                .filter(Objects::nonNull)
                .map(event -> new EventDetailDto(
                        event.getId(),
                        event.getMusicProject().getId(),
                        event.getTitle(),
                        event.getDescription(),
                        event.getStartAt().toLocalDate(),
                        event.getStartAt().toLocalTime(),
                        event.getLocation(),
                        event.getMusicProject().getName(),
                        event.getMusicProject().getProfileImage(),
                        eventRepository.countParticipantsByEventId(event.getId()),
                        eventRepository.countSongsByEventId(event.getId(), SetlistItemType.SONG),
                        List.of()
                ))
                .toList();
    }

    private MusicProject buildProject(MusicProjectCreateDTO dto, User creator) {
        MusicProject project = new MusicProject();
        project.setName(dto.getName());
        project.setType(dto.getType());
        project.setCreatedByUserId(creator.getId());
        project.setCreatedAt(LocalDateTime.now());
        return project;
    }

    private void addOwnerMember(MusicProject project, User creator) {
        validIfMemberExistInProject(project.getId(), creator.getId());

        MusicProjectMember ownerMember = new MusicProjectMember();
        ownerMember.setMusicProject(project);
        ownerMember.setUser(creator);
        ownerMember.setAddedByUserId(creator.getId());
        ownerMember.setProjectRole(ProjectMemberRole.OWNER);
        ownerMember.setCreatedAt(LocalDateTime.now());

        musicProjectMemberRepository.save(ownerMember);
    }

    private void validIfMemberExistInProject(UUID projectId, UUID userId) {
        boolean alreadyMember = musicProjectMemberRepository
                .existsByMusicProject_IdAndUser_Id(projectId, userId);
        if (alreadyMember) {
            throw new ValidationException("Usuário já é membro deste projeto.");
        }
    }

    private MusicProjectDetailDTO toDetailDto(MusicProject project) {
        MusicProjectDetailDTO out = new MusicProjectDetailDTO();
        out.setId(project.getId());
        out.setName(project.getName());
        out.setType(project.getType());
        out.setProfileImage(project.getProfileImage());
        out.setProfileImageHash(project.getProfileImageHash());

        List<MemberDTO> members = project.getMembers()
                .stream()
                .map(member -> {
                    User user = member.getUser();
                    MemberDTO dto = new MemberDTO();
                    dto.setId(user.getId());
                    dto.setFirstName(user.getFirstName());
                    dto.setLastName(user.getLastName());
                    dto.setProfileImage(user.getProfileImage());
                    dto.setProjectRole(member.getProjectRole());
                    return dto;
                })
                .toList();

        out.setMembers(members);
        return out;
    }

    @Override
    @Transactional
    public void assignSkillsToMember(UUID projectId, UUID memberId, List<UUID> skillIds) {
        MusicProjectMember member = musicProjectMemberRepository.findById(memberId)
                .filter(m -> m.getMusicProject().getId().equals(projectId))
                .orElseThrow(() -> new ValidationException("Membro não encontrado neste projeto."));

        List<ProjectSkill> selectedSkills = projectSkillRepository.findAllById(skillIds);

        boolean allSkillsFromProject = selectedSkills.stream()
                .allMatch(s -> s.getMusicProject().getId().equals(projectId));

        if (!allSkillsFromProject) {
            throw new ValidationException("Uma ou mais funções selecionadas não pertencem a este projeto.");
        }
        member.getProjectSkills().clear();
        member.getProjectSkills().addAll(selectedSkills);

        musicProjectMemberRepository.save(member);
    }

    @Override
    @Transactional
    public void addProjectSkill(UUID projectId, ProjectSkillRequestDTO skillDto) {
        MusicProject project = musicProjectRepository.findById(projectId)
                .orElseThrow(() -> new ValidationException("Projeto musical não encontrado."));

        ProjectSkill skill = new ProjectSkill();
        skill.setName(skillDto.name());
        skill.setIconKey(skillDto.iconKey());
        skill.setMusicProject(project);

        projectSkillRepository.save(skill);
    }

    @Override
    @Transactional
    public ProjectSkillDTO updateProjectSkill(UUID projectId, UUID skillId, ProjectSkillRequestDTO skillDto) {
        ProjectSkill skill = projectSkillRepository.findById(skillId)
                .orElseThrow(() -> new ValidationException("Função não encontrada."));

        if (!skill.getMusicProject().getId().equals(projectId)) {
            throw new ValidationException("Esta função não pertence ao projeto informado.");
        }

        skill.setName(skillDto.name());
        skill.setIconKey(skillDto.iconKey());

        return ProjectSkillDTO.fromEntity(projectSkillRepository.save(skill));
    }

    @Override
    public List<ProjectSkillDTO> getProjectSkills(UUID projectId) {
        return projectSkillRepository.findByMusicProject_Id(projectId)
                .stream()
                .map(ProjectSkillDTO::fromEntity)
                .toList();
    }

    @Override
    public MemberDTO getMember(UUID projectId, UUID memberId) {
        MusicProjectMember member = musicProjectMemberRepository
                .findById(memberId)
                .orElseThrow(() -> new ValidationException("O usuário não é membro deste projeto."));
        return memberMapper.toDto(member);
    }

    @Override
    public MemberDTO updateMember(UUID projectId, UUID memberId, UpdateMemberRequest request) {
        MusicProjectMember member = musicProjectMemberRepository
                .findById(memberId)
                .orElseThrow(() -> new ValidationException("O usuário não é membro deste projeto."));
        member.setProjectRole(request.projectRole());
        Set<ProjectSkill> newSkills = new HashSet<>(
                projectSkillRepository.findAllById(request.skillIds())
        );
        member.setProjectSkills(newSkills);
        return memberMapper.toDto(musicProjectMemberRepository.save(member));
    }

    @Override
    @Transactional
    public void deleteMember(UUID projectId, UUID memberId) {
        MusicProjectMember member = musicProjectMemberRepository.findById(memberId)
                .filter(m -> m.getMusicProject().getId().equals(projectId))
                .orElseThrow(() -> new ValidationException("O usuário não é membro deste projeto."));

        ProjectMemberRole currentRole = getMemberRole(projectId);
        if (currentRole == ProjectMemberRole.MEMBER) {
            throw new ValidationException("Você não tem permissão para remover membros.");
        }
        if (member.getProjectRole() == ProjectMemberRole.OWNER) {
            throw new ValidationException("Não é possível remover o proprietário do projeto.");
        }

        List<EventParticipant> participants = eventParticipantRepository.findByMember_Id(memberId);
        if (!participants.isEmpty()) {
            List<UUID> participantIds = participants.stream()
                    .map(EventParticipant::getId)
                    .toList();
            eventSetlistItemRepository.deleteByAddedBy_IdIn(participantIds);
            eventParticipantRepository.deleteAllInBatch(participants);
        }

        musicProjectMemberRepository.delete(member);
    }

    @Override
    public ProjectMemberRole getMemberRole(UUID projectId) {
        User currentUser = currentUserProvider.get();
        Optional<MusicProjectMember> member = musicProjectMemberRepository.findByMusicProject_IdAndUser_Id(projectId, currentUser.getId());
        return member.get().getProjectRole();
    }

    @Override
    public MonthOverviewResponse getMonthOverview(UUID projectId, String yearMonth) {
        List<Event> events = getEventsOfMonth(projectId, yearMonth);

        if (events.isEmpty()) {
            return new MonthOverviewResponse(projectId, yearMonth, 0, List.of());
        }

        List<UUID> eventIds = events.stream().map(Event::getId).toList();

        List<EventParticipant> allParticipants = eventParticipantRepository.findByEventIdIn(eventIds);
        List<EventSetlistItem> allSongs = eventSetlistItemRepository.findByEventIdInAndType(eventIds, SetlistItemType.SONG);

        Map<UUID, List<EventParticipant>> participantsByEvent = allParticipants.stream()
                .collect(java.util.stream.Collectors.groupingBy(p -> p.getEvent().getId()));

        Map<UUID, List<EventSetlistItem>> songsByEvent = allSongs.stream()
                .collect(java.util.stream.Collectors.groupingBy(s -> s.getEvent().getId()));

        List<MonthEventItem> items = events.stream().map(event -> {
            MonthEventItem base = eventOverviewMapper.toMonthItem(event);

            var participants = participantsByEvent
                    .getOrDefault(event.getId(), List.of())
                    .stream()
                    .map(eventParticipantMapper::toMonthOverviewParticipant)
                    .toList();

            var songs = songsByEvent
                    .getOrDefault(event.getId(), List.of())
                    .stream()
                    .map(eventSetlistItemMapper::toMonthOverviewSong)
                    .toList();

            return new MonthEventItem(
                    base.eventId(),
                    base.eventName(),
                    base.day(),
                    base.time(),
                    participants,
                    songs
            );
        }).toList();

        return new MonthOverviewResponse(projectId, yearMonth, items.size(), items);
    }

    List<Event> getEventsOfMonth(UUID projectId, String yearMonth) {
        YearMonth ym;
        try {
            ym = YearMonth.parse(yearMonth);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Formato inválido para yearMonth. Use YYYY-MM (ex: 2026-02)");
        }

        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

        return eventRepository.findAllByMusicProjectIdAndStartAtBetweenOrderByStartAtAsc(projectId, start, end);
    }
}
