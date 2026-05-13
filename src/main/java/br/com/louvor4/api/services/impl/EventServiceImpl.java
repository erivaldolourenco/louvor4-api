package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.EventPermission;
import br.com.louvor4.api.enums.EventParticipantStatus;
import br.com.louvor4.api.enums.NotificationType;
import br.com.louvor4.api.enums.SetlistItemType;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.mapper.EventMapper;
import br.com.louvor4.api.mapper.EventSetlistItemMapper;
import br.com.louvor4.api.models.*;
import br.com.louvor4.api.repositories.*;
import br.com.louvor4.api.repositories.projections.EventCountProjection;
import br.com.louvor4.api.services.EventReminderScheduler;
import br.com.louvor4.api.services.EventService;
import br.com.louvor4.api.services.PushSenderService;
import br.com.louvor4.api.services.ProgramService;
import br.com.louvor4.api.services.UserNotificationService;
import br.com.louvor4.api.shared.dto.Event.EventDetailDto;
import br.com.louvor4.api.shared.dto.Event.EventParticipantDTO;
import br.com.louvor4.api.shared.dto.Event.EventParticipantResponseDTO;
import br.com.louvor4.api.shared.dto.Event.SetlistDTO;
import br.com.louvor4.api.shared.dto.Event.UpdateEventDto;
import br.com.louvor4.api.shared.dto.Event.UserEventDetailDto;
import br.com.louvor4.api.shared.dto.Song.AddEventSetlistItemDTO;
import br.com.louvor4.api.shared.dto.notification.CreateUserNotificationRequest;
import br.com.louvor4.api.strategy.event.EventSetlistItemStrategy;
import br.com.louvor4.api.strategy.event.EventSetlistItemStrategyResolver;
import br.com.louvor4.api.validations.EventValidation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final MusicProjectMemberRepository musicProjectMemberRepository;
    private final EventMapper eventMapper;
    private final EventSetlistItemMapper eventSetlistItemMapper;
    private final CurrentUserProvider currentUserProvider;
    private final ProjectSkillRepository projectSkillRepository;
    private final SongRepository songRepository;
    private final EventSetlistItemRepository eventSetlistItemRepository;
    private final PushSenderService senderService;
    private final UserNotificationService userNotificationService;
    private final UserUnavailabilityRepository userUnavailabilityRepository;
    private final EventSetlistItemStrategyResolver strategyResolver;
    private final ProgramService programService;
    private final EventReminderScheduler eventReminderScheduler;

    private final EventValidation eventValidation = new EventValidation();



    public EventServiceImpl(
            EventRepository eventRepository,
            EventParticipantRepository eventParticipantRepository,
            MusicProjectMemberRepository musicProjectMemberRepository, EventMapper eventMapper, EventSetlistItemMapper eventSetlistItemMapper, CurrentUserProvider currentUserProvider, ProjectSkillRepository projectSkillRepository, SongRepository songRepository, EventSetlistItemRepository eventSetlistItemRepository, PushSenderService senderService, UserNotificationService userNotificationService, UserUnavailabilityRepository userUnavailabilityRepository, EventSetlistItemStrategyResolver strategyResolver,
            ProgramService programService,
            EventReminderScheduler eventReminderScheduler
    ) {
        this.eventRepository = eventRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.musicProjectMemberRepository = musicProjectMemberRepository;
        this.eventMapper = eventMapper;
        this.eventSetlistItemMapper = eventSetlistItemMapper;
        this.currentUserProvider = currentUserProvider;
        this.projectSkillRepository = projectSkillRepository;
        this.songRepository = songRepository;
        this.eventSetlistItemRepository = eventSetlistItemRepository;
        this.senderService = senderService;
        this.userNotificationService = userNotificationService;
        this.userUnavailabilityRepository = userUnavailabilityRepository;
        this.strategyResolver = strategyResolver;
        this.programService = programService;
        this.eventReminderScheduler = eventReminderScheduler;
    }

    @Transactional
    @Override
    public void addOrUpdateParticipantsToEvent(UUID eventId, List<EventParticipantDTO> participantsDto) {
        Event event = findEventOrThrow(eventId);
        List<EventParticipantDTO> incoming = (participantsDto == null) ? List.of() : participantsDto;

        eventValidation.validateRequestAllowEmpty(incoming);

        List<EventParticipant> currentList = eventParticipantRepository.findByEventId(eventId);

        if (incoming.isEmpty()) {
            if (!currentList.isEmpty()) eventParticipantRepository.deleteAllInBatch(currentList);
            return;
        }

        Map<UUID, EventParticipant> currentByMemberId = currentList.stream()
                .collect(Collectors.toMap(p -> p.getMember().getId(), p -> p));

        List<EventParticipant> toSave = new ArrayList<>(incoming.size());
        Set<UUID> incomingMemberIds = new HashSet<>(incoming.size());

        List<EventParticipant> newParticipants = new ArrayList<>();

        for (EventParticipantDTO dto : incoming) {
            UUID memberId = dto.getMemberId();
            incomingMemberIds.add(memberId);
            EventParticipant existing = currentByMemberId.get(memberId);

            MusicProjectMember member = (existing != null) ? existing.getMember() : findMemberOrThrow(memberId);
            ProjectSkill skill = (dto.getSkillId() != null) ? validateAndGetSkill(member, dto.getSkillId()) : null;
            Set<EventPermission> perms = normalizePermissions(dto.getPermissions());

            if (existing != null) {
                existing.setSkill(skill);
                existing.setPermissions(perms);
                toSave.add(existing);
            } else {
                validateMemberAvailabilityForEvent(event, member);
                EventParticipant newParticipant = createParticipant(event, member, skill, perms);
                toSave.add(newParticipant);
                newParticipants.add(newParticipant);
            }
        }

        List<EventParticipant> toDelete = currentList.stream()
                .filter(p -> !incomingMemberIds.contains(p.getMember().getId()))
                .toList();
        if (!toDelete.isEmpty()) eventParticipantRepository.deleteAllInBatch(toDelete);

        eventParticipantRepository.saveAll(toSave);

        notifyNewParticipants(event, newParticipants);
    }

    @Override
    @Transactional
    public void acceptParticipation(UUID participantId) {
        updateParticipationStatus(participantId, EventParticipantStatus.ACCEPTED);
    }

    @Override
    @Transactional
    public void declineParticipation(UUID participantId) {
        updateParticipationStatus(participantId, EventParticipantStatus.DECLINED);
    }

    private void notifyNewParticipants(Event event, List<EventParticipant> newParticipants) {
        String eventTitle = (event.getTitle() == null || event.getTitle().isBlank())
                ? "Evento"
                : event.getTitle().trim();
        String title = "Convite para escala: " + eventTitle + " - " + formatEventDate(event.getStartAt());

        for (EventParticipant participant : newParticipants) {
            String message = buildParticipantNotificationMessage(event, participant);
            UUID userId = participant.getMember().getUser().getId();
            userNotificationService.createNotification(new CreateUserNotificationRequest(
                    NotificationType.EVENT_INVITE,
                    userId,
                    title,
                    message,
                    participant.getId(),
                    null
            ));

            try {
                senderService.sendToUser(userId, title, message);
            } catch (Exception e) {
                System.err.println("Falha ao enviar push para usuário: " + e.getMessage());
            }
        }
    }

    private void updateParticipationStatus(UUID participantId, EventParticipantStatus status) {
        if (participantId == null) {
            throw new ValidationException("Id do participante é obrigatório.");
        }

        UUID userId = currentUserProvider.get().getId();
        Optional<EventParticipant> optionalParticipant =
                eventParticipantRepository.findByIdAndMemberUserId(participantId, userId);
        if (optionalParticipant.isEmpty()) {
            userNotificationService.markInviteAsReadByEventParticipantIdIfExists(userId, participantId);
            throw new NotFoundException("Participação no evento não encontrada.");
        }

        EventParticipant participant = optionalParticipant.get();

        if (participant.getEvent() != null && participant.getEvent().getStartAt() != null
                && !participant.getEvent().getStartAt().isAfter(LocalDateTime.now())) {
            throw new ValidationException("Não é possível responder a participação após o início do evento.");
        }

        if (participant.getStatus() == status) {
            userNotificationService.markInviteAsReadByEventParticipantId(userId, participant.getId());
            return;
        }

        if (participant.getStatus() != EventParticipantStatus.PENDING) {
            throw new ValidationException("A participação já foi respondida.");
        }

        participant.setStatus(status);
        eventParticipantRepository.save(participant);
        userNotificationService.markInviteAsReadByEventParticipantId(userId, participant.getId());
        notifyProjectOwnerAboutParticipantDecision(participant, status);
    }

    private void notifyProjectOwnerAboutParticipantDecision(EventParticipant participant, EventParticipantStatus status) {
        Event event = participant.getEvent();
        UUID ownerUserId = event.getMusicProject().getCreatedByUserId();
        UUID participantUserId = participant.getMember().getUser().getId();

        if (ownerUserId == null || ownerUserId.equals(participantUserId)) {
            return;
        }

        String participantName = buildParticipantName(participant.getMember().getUser());
        String title = "Resposta de participação: " + event.getTitle();
        String message = switch (status) {
            case ACCEPTED -> participantName + " aceitou participar do evento.";
            case DECLINED -> participantName + " recusou participar do evento.";
            default -> participantName + " atualizou a participação no evento.";
        };
        NotificationType notificationType = switch (status) {
            case ACCEPTED -> NotificationType.EVENT_PARTICIPANT_ACCEPTED;
            case DECLINED -> NotificationType.EVENT_PARTICIPANT_DECLINED;
            default -> NotificationType.SYSTEM_NOTIFICATION;
        };

        userNotificationService.createNotification(new CreateUserNotificationRequest(
                notificationType,
                ownerUserId,
                title,
                message,
                participant.getId(),
                null
        ));

        try {
            senderService.sendToUser(ownerUserId, title, message);
        } catch (Exception e) {
            System.err.println("Falha ao enviar push para o responsável do projeto: " + e.getMessage());
        }
    }

    private String buildParticipantNotificationMessage(Event event, EventParticipant participant) {
        String eventTitle = (event.getTitle() == null || event.getTitle().isBlank())
                ? "evento"
                : "'" + event.getTitle().trim() + "'";
        String when = formatEventDateTime(event.getStartAt());
        String location = (event.getLocation() == null || event.getLocation().isBlank())
                ? "local a definir"
                : event.getLocation().trim();
        String role = (participant.getSkill() == null || participant.getSkill().getName() == null || participant.getSkill().getName().isBlank())
                ? ""
                : " Função prevista: " + participant.getSkill().getName().trim() + ".";

        return "Você foi convidado para " + eventTitle
                + " em " + when
                + ", local: " + location + "."
                + role
                + " Confirme sua participação no app (Aceitar ou Recusar).";
    }

    private String formatEventDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "data/horário a definir";
        var dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        var timeFormatter = java.time.format.DateTimeFormatter.ofPattern("HH:mm");
        return dateTime.format(dateFormatter) + " às " + dateTime.format(timeFormatter);
    }

    private String formatEventDate(LocalDateTime dateTime) {
        if (dateTime == null) return "data a definir";
        var dateFormatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        return dateTime.format(dateFormatter);
    }

    private Event findEventOrThrow(UUID eventId) {
        if (eventId == null) {
            throw new ValidationException("Id do evento é obrigatório.");
        }
        return eventRepository.findById(eventId) .orElseThrow(() -> new ValidationException("Evento não encontrado.") );
    }

    private MusicProjectMember findMemberOrThrow(UUID memberId) {
        return musicProjectMemberRepository.findById(memberId)
                .orElseThrow(() -> new ValidationException("Membro não encontrado"));
    }

    private Set<EventPermission> normalizePermissions(Set<EventPermission> permissions) {
        return (permissions == null || permissions.isEmpty())
                ? EnumSet.noneOf(EventPermission.class)
                : EnumSet.copyOf(permissions);
    }

    private ProjectSkill validateAndGetSkill(MusicProjectMember member, UUID skillId) {
        ProjectSkill skill = projectSkillRepository.findById(skillId)
                .orElseThrow(() -> new ValidationException("Função (Skill) não encontrada."));

        if (!member.getProjectSkills().contains(skill)) {
            throw new ValidationException("O membro não possui aptidão para esta função.");
        }
        return skill;
    }

    private EventParticipant createParticipant(Event event, MusicProjectMember member, ProjectSkill skill, Set<EventPermission> perms) {
        EventParticipant participant = new EventParticipant();
        participant.setEvent(event);
        participant.setMember(member);
        participant.setSkill(skill);
        participant.setPermissions(normalizePermissions(perms));
        participant.setStatus(EventParticipantStatus.PENDING);
        return participant;
    }

    private void validateMemberAvailabilityForEvent(Event event, MusicProjectMember member) {
        if (event == null || event.getStartAt() == null || member == null || member.getUser() == null) {
            return;
        }

        UUID userId = member.getUser().getId();
        UUID projectId = event.getMusicProject() != null ? event.getMusicProject().getId() : null;
        var eventDate = event.getStartAt().toLocalDate();

        List<UserUnavailability> unavailabilities = userUnavailabilityRepository.findActiveByUserIdAndEventDate(
                userId,
                eventDate
        );

        boolean unavailable = unavailabilities.stream().anyMatch(unavailability ->
                Boolean.TRUE.equals(unavailability.getAppliesToAllProjects()) ||
                        unavailability.getProjects().stream()
                                .map(UserUnavailabilityProject::getProject)
                                .filter(Objects::nonNull)
                                .map(MusicProject::getId)
                                .anyMatch(candidateProjectId -> Objects.equals(candidateProjectId, projectId))
        );

        if (unavailable) {
            throw new ValidationException("O "+member.getUser().getFirstName()+" "+member.getUser().getLastName()+" está indisponível na data do evento.");
        }
    }

    private String buildParticipantName(User user) {
        if (user == null) {
            return "O participante";
        }

        String firstName = user.getFirstName() == null ? "" : user.getFirstName().trim();
        String lastName = user.getLastName() == null ? "" : user.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        return fullName.isBlank() ? "O participante" : fullName;
    }


    @Override
    public List<UserEventDetailDto> getEventsByUser() {
        UUID userId = currentUserProvider.get().getId();

        List<EventParticipant> eventsParticipant = eventParticipantRepository
                .findAcceptedByUserWithEventAndProjectAndMemberUser(
                        userId,
                        EventParticipantStatus.ACCEPTED,
                        LocalDateTime.now().minusDays(1)
                );

        List<Event> events = eventsParticipant.stream()
                .map(EventParticipant::getEvent)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (events.isEmpty()) {
            return List.of();
        }

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

        Map<UUID, EventParticipant> acceptedParticipantByEventId = eventsParticipant.stream()
                .filter(participant -> participant.getEvent() != null)
                .collect(Collectors.toMap(
                        participant -> participant.getEvent().getId(),
                        participant -> participant,
                        (left, right) -> left
                ));

        Map<UUID, List<String>> participantsImagesByEvent = buildParticipantsImagesByEvent(eventIds);

        return events.stream()
                .map(event -> {
                    EventParticipant participant = acceptedParticipantByEventId.get(event.getId());
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
                        participant != null ? participant.getId() : null,
                        participant != null ? participant.getStatus() : null
                    );
                })
                .toList();
    }

    private Map<UUID, List<String>> buildParticipantsImagesByEvent(List<UUID> eventIds) {
        if (eventIds.isEmpty()) {
            return Map.of();
        }
        return eventParticipantRepository.findProfileImagesByEventIds(eventIds)
                .stream()
                .map(p -> Map.entry(
                        p.getEventId(),
                        p.getProfileImage()
                ))
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey,
                        Collectors.mapping(Map.Entry::getValue, Collectors.toList())
                ));
    }

    @Override
    public EventDetailDto getEventById(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ValidationException("Evento não encontrado."));
        return eventMapper.toDetailDto(event);
    }

    @Override
    public List<EventParticipantResponseDTO> getParticipants(UUID eventId) {
        List<EventParticipant> participants = eventParticipantRepository.findByEventId(eventId);
        return participants.stream()
                .map(p -> new EventParticipantResponseDTO(
                        p.getId(),
                        p.getMember().getId(),
                        p.getMember().getUser().getFirstName(),
                        p.getMember().getUser().getLastName(),
                        p.getMember().getUser().getProfileImage(),
                        p.getSkill() != null ? p.getSkill().getId() : null,
                        p.getPermissions(),
                        p.getStatus()
                ))
                .toList();
    }

    @Override
    public void addSetListItemToEvent(UUID eventId, List<AddEventSetlistItemDTO> addEventSetlistItemDto) {
        if (addEventSetlistItemDto == null || addEventSetlistItemDto.isEmpty()) {
            throw new ValidationException("A lista de músicas é obrigatória.");
        }

        EventParticipant participant =  validateEventParticipant(eventId);

        Set<UUID> setListItemIds = new LinkedHashSet<>();
        for (AddEventSetlistItemDTO dto : addEventSetlistItemDto) {
            if (dto == null || dto.itemId() == null || dto.type() == null) {
                throw new ValidationException("itemId e type são obrigatórios.");
            }
            if (!setListItemIds.add(dto.itemId())) {
                throw new ValidationException("Música duplicada no payload: " + dto.itemId());
            }
        }

        Set<UUID> existingSongIds = eventSetlistItemRepository
                .findByEventIdAndTypeOrderBySequenceAsc(eventId, SetlistItemType.SONG)
                .stream()
                .map(es -> es.getSong().getId())
                .collect(Collectors.toSet());

        for (UUID songId : setListItemIds) {
            if (existingSongIds.contains(songId)) {
                throw new ValidationException("Música já adicionada ao evento: " + songId);
            }
        }


        Integer maxSequence = eventSetlistItemRepository.findMaxSequenceByEventId(eventId);
        int nextSequence = maxSequence == null ? 1 : maxSequence + 1;

        List<EventSetlistItem> toSave = new ArrayList<>(setListItemIds.size());
        for (AddEventSetlistItemDTO dto : addEventSetlistItemDto) {

            EventSetlistItemStrategy strategy = strategyResolver.resolve(dto.type());

            EventSetlistItem eventSetlistItem = strategy.create(dto.itemId(), participant, nextSequence++);

            toSave.add(eventSetlistItem);
        }

        eventSetlistItemRepository.saveAll(toSave);
        for (EventSetlistItem saved : toSave) {
            programService.onSetlistItemAdded(saved);
        }
    }

    private EventParticipant validateEventParticipant(UUID eventId){
        User user = currentUserProvider.get();

        EventParticipant participant = eventParticipantRepository
                .findByEventIdAndMemberUserId(eventId, user.getId())
                .orElseThrow(() -> new ValidationException("Usuário não está escalado como participante deste evento."));

        eventValidation.canAddSong(participant);

        return participant;
    }


    @Override
    public List<SetlistDTO> getSetlist(UUID eventId) {
        findEventOrThrow(eventId);
        List<EventSetlistItem> setlistItems = eventSetlistItemRepository
                .findByEventIdOrderBySequenceAsc(eventId);
        return eventSetlistItemMapper.toSetlistDtoList(setlistItems);
    }

    @Override
    @Transactional
    public void removeSetlistItemFromEvent(UUID eventId, UUID setlistItemId) {

        User user = currentUserProvider.get();

        EventParticipant participant = eventParticipantRepository
                .findByEventIdAndMemberUserId(eventId, user.getId())
                .orElseThrow(() -> new ValidationException("Usuário não está escalado como participante deste evento."));

        eventValidation.canAddSong(participant);
        EventSetlistItem setlistItem = eventSetlistItemRepository.findById(setlistItemId)
                .orElseThrow(() -> new NotFoundException("Item do setlist não encontrado no evento."));

        eventValidation.validateSetlistItemBelongsToEvent(setlistItem, eventId);
        programService.onSetlistItemRemoved(setlistItem.getId());
        eventSetlistItemRepository.delete(setlistItem);
    }

    @Override
    public void deleteEventById(UUID eventId) {

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ValidationException("Evento não encontrado."));
        Integer participants = eventRepository.countParticipantsByEventId(eventId);
        Integer songs = eventRepository.countSongsByEventId(eventId, SetlistItemType.SONG);
        eventValidation.validateDeletionRules(participants, songs);

        eventReminderScheduler.cancel(eventId);
        eventRepository.delete(event);
    }

    @Override
    public void updateEventBy(UUID eventId, UpdateEventDto eventDto) {
        Event event = findEventOrThrow(eventId);
        eventMapper.updateEntityFromDto(eventDto, event);
        Event saved = eventRepository.save(event);
        eventReminderScheduler.reschedule(saved);
    }


}
