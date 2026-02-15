package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.FileCategory;
import br.com.louvor4.api.enums.ProjectMemberRole;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.mapper.*;
import br.com.louvor4.api.models.*;
import br.com.louvor4.api.repositories.*;
import br.com.louvor4.api.services.MusicProjectService;
import br.com.louvor4.api.services.StorageService;
import br.com.louvor4.api.services.UserService;
import br.com.louvor4.api.shared.dto.Event.CreateEventDto;
import br.com.louvor4.api.shared.dto.Event.EventDetailDto;
import br.com.louvor4.api.shared.dto.MusicProject.*;
import br.com.louvor4.api.shared.dto.eventOverview.MonthEventItem;
import br.com.louvor4.api.shared.dto.eventOverview.MonthOverviewResponse;
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
    private final MusicProjectMemberMapper musicProjectMemberMapper;
    private final EventMapper eventMapper;
    private final EventSongMapper eventSongMapper;
    private final EventParticipantMapper eventParticipantMapper;
    private final EventOverviewMapper eventOverviewMapper;
    private final EventRepository eventRepository;
    private final ProjectSkillRepository projectSkillRepository;
    private final MemberMapper memberMapper;
    private final EventParticipantRepository eventParticipantRepository;
    private final EventSongRepository eventSongRepository;


    public MusicProjectServiceImpl(MusicProjectRepository musicProjectRepository, MusicProjectMemberRepository musicProjectMemberRepository, CurrentUserProvider currentUserProvider, StorageService storageService, UserService userService, MusicProjectMemberMapper musicProjectMemberMapper, EventMapper eventMapper, EventSongMapper eventSongMapper, EventParticipantMapper eventParticipantMapper, EventOverviewMapper eventOverviewMapper, EventRepository eventRepository, ProjectSkillRepository projectSkillRepository, MemberMapper memberMapper, EventParticipantRepository eventParticipantRepository, EventSongRepository eventSongRepository) {
        this.musicProjectRepository = musicProjectRepository;
        this.musicProjectMemberRepository = musicProjectMemberRepository;
        this.currentUserProvider = currentUserProvider;
        this.storageService = storageService;
        this.userService = userService;
        this.musicProjectMemberMapper = musicProjectMemberMapper;
        this.eventMapper = eventMapper;
        this.eventSongMapper = eventSongMapper;
        this.eventParticipantMapper = eventParticipantMapper;
        this.eventOverviewMapper = eventOverviewMapper;
        this.eventRepository = eventRepository;
        this.projectSkillRepository = projectSkillRepository;
        this.memberMapper = memberMapper;
        this.eventParticipantRepository = eventParticipantRepository;
        this.eventSongRepository = eventSongRepository;
    }

    @Override
    @Transactional
    public MusicProjectDetailDTO
    create(MusicProjectCreateDTO dto) {
        User creator = currentUserProvider.get();

        MusicProject project = buildProject(dto, creator);
        project = musicProjectRepository.save(project);

        addOwnerMember(project, creator);

        return toDetailDto(project);
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
    public String updateImage(UUID projecId, MultipartFile profileImage){
        MusicProject musicProject = musicProjectRepository.findById(projecId)
                .orElseThrow(() -> new EntityNotFoundException("MusicProject não encontrado: " + projecId));
        if (isNotNullOrEmpty(profileImage)) {
            String fileUrl = storageService.uploadFile(profileImage, FileCategory.PROJECT_PROFILE);
            musicProject.setProfileImage(fileUrl);
        }
        MusicProject saved = musicProjectRepository.save(musicProject);

        return saved.getProfileImage();
    }


    @Override
    public MusicProjectDetailDTO getById(UUID projectId) {
        MusicProject musicProject =  musicProjectRepository.getMusicProjectById(projectId);
        return toDetailDto(musicProject);
    }

    @Override
    public List<MusicProjectDTO> getFromUser() {

        List<MusicProjectMember> musicProjectMember = musicProjectMemberRepository.getMusicProjectMembersByUser_Id(currentUserProvider.get().getId());

        return musicProjectMember.stream()
                .map(MusicProjectMember::getMusicProject)
                .distinct()
                .map(project -> {
                    MusicProjectDTO dto = new MusicProjectDTO();
                    dto.setId(project.getId());
                    dto.setName(project.getName());
                    dto.setType(project.getType());
                    dto.setProfileImage(project.getProfileImage());
                    return dto;
                })
                .toList();
    }

    @Override
    public void addMember(UUID projectId, AddMemberDTO addDto) {

        User user = userService.findByUsername(addDto.getUsername());

        validIfMemberExistInProject(projectId, user.getId());

        User creator = currentUserProvider.get();
        User userMember = userService.findUserById(user.getId());
        MusicProject musicProject = musicProjectRepository.getMusicProjectById(projectId);
        MusicProjectMember musicProjectMember = new MusicProjectMember();
        musicProjectMember.setUser(userMember);
        musicProjectMember.setMusicProject(musicProject);
        musicProjectMember.setAddedByUserId(creator.getId());
        musicProjectMember.setProjectRole(ProjectMemberRole.MEMBER);

        musicProjectMemberRepository.save(musicProjectMember);
    }

    @Override
    public List<MemberDTO> getMembers(UUID projectId) {
        List<MusicProjectMember> musicProjectMembers =  musicProjectMemberRepository.getMusicProjectMembersByMusicProject_Id(projectId);
        return musicProjectMemberMapper.toDtoList(musicProjectMembers);
    }

    @Override
    public CreateEventDto createEvent(UUID projectId, CreateEventDto eventDto) {
        MusicProject project = musicProjectRepository.findById(projectId)
                .orElseThrow(() -> new ValidationException("Projeto não encontrado."));
        Event event = eventMapper.toEntity(eventDto);
        event.setMusicProject(project);
        Event saved = eventRepository.save(event);
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
                        eventRepository.countSongsByEventId(event.getId())
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

    private void validIfMemberExistInProject(UUID projectId, UUID userId){
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
        skill.setMusicProject(project);

        projectSkillRepository.save(skill);
    }

    @Override
    public List<ProjectSkillDTO> getProjectSkills(UUID projectId) {
        return projectSkillRepository.findByMusicProject_Id(projectId)
                .stream()
                .map(skill -> new ProjectSkillDTO(
                        skill.getId(),
                        skill.getName()
                ))
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
        List<EventSong> allSongs = eventSongRepository.findByEventIdIn(eventIds);

        Map<UUID, List<EventParticipant>> participantsByEvent = allParticipants.stream()
                .collect(java.util.stream.Collectors.groupingBy(p -> p.getEvent().getId()));

        Map<UUID, List<EventSong>> songsByEvent = allSongs.stream()
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
                    .map(eventSongMapper::toMonthOverviewSong)
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

    List<Event> getEventsOfMonth(UUID projectId, String yearMonth){
        YearMonth ym;
        try {
            ym = YearMonth.parse(yearMonth);
        } catch (DateTimeParseException e) {
            throw new ValidationException("Formato inválido para yearMonth. Use YYYY-MM (ex: 2026-02)");
        }

        LocalDateTime start = ym.atDay(1).atStartOfDay();
        LocalDateTime end = ym.plusMonths(1).atDay(1).atStartOfDay();

        List<Event> events =  eventRepository.findAllByMusicProjectIdAndStartAtBetweenOrderByStartAtAsc(projectId, start, end);
        return events;
    }

}
