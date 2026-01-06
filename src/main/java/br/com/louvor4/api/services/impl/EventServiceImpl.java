package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.EventPermission;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.mapper.EventMapper;
import br.com.louvor4.api.models.Event;
import br.com.louvor4.api.models.EventParticipant;
import br.com.louvor4.api.models.User;
import br.com.louvor4.api.repositories.EventParticipantRepository;
import br.com.louvor4.api.repositories.EventRepository;
import br.com.louvor4.api.repositories.MusicProjectMemberRepository;
import br.com.louvor4.api.repositories.UserRepository;
import br.com.louvor4.api.services.EventService;
import br.com.louvor4.api.shared.dto.Event.CreateEventDto;
import br.com.louvor4.api.shared.dto.Event.EventDetailDto;
import br.com.louvor4.api.shared.dto.Event.EventParticipantDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final MusicProjectMemberRepository musicProjectMemberRepository;
    private final EventMapper eventMapper;
    private final CurrentUserProvider currentUserProvider;


    public EventServiceImpl(
            EventRepository eventRepository,
            UserRepository userRepository,
            EventParticipantRepository eventParticipantRepository,
            MusicProjectMemberRepository musicProjectMemberRepository, EventMapper eventMapper, CurrentUserProvider currentUserProvider
    ) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.musicProjectMemberRepository = musicProjectMemberRepository;
        this.eventMapper = eventMapper;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    @Override
    public void addParticipantToEvent(UUID eventId, EventParticipantDTO participantDto) {

        if (participantDto == null || participantDto.getUserId() == null) {
            throw new ValidationException("Informe o userId do participante.");
        }

        // 1) evento existe?
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ValidationException("Evento não encontrado."));

        // 2) usuário existe?
        User user = userRepository.findById(participantDto.getUserId())
                .orElseThrow(() -> new ValidationException("Usuário não encontrado."));

        // 3) usuário pertence ao projeto do evento?
        UUID projectId = event.getMusicProject().getId();

        boolean isProjectMember = musicProjectMemberRepository
                .existsByMusicProject_IdAndUser_Id(projectId, user.getId());

        if (!isProjectMember) {
            throw new ValidationException("Usuário não faz parte do projeto deste evento.");
        }

        // 4) já existe como participante?
        boolean alreadyParticipant = eventParticipantRepository
                .existsByEvent_IdAndUser_Id(event.getId(), user.getId());

        if (alreadyParticipant) {
            throw new ValidationException("Usuário já está adicionado neste evento.");
        }

        // 5) permissões (padrão = vazio)
        Set<EventPermission> perms = participantDto.getPermissions();
        EnumSet<EventPermission> permissions = (perms == null || perms.isEmpty())
                ? EnumSet.noneOf(EventPermission.class)
                : EnumSet.copyOf(perms);

        // 6) cria e salva
        EventParticipant participant = new EventParticipant();
        participant.setEvent(event);
        participant.setUser(user);
        participant.setPermissions(permissions);

        eventParticipantRepository.save(participant);
    }

    @Override
    public List<EventDetailDto> getEventsByUser() {
        UUID userId =  currentUserProvider.get().getId();
        List<EventParticipant> eventsParticipant = eventParticipantRepository.findByUser_IdOrderByEvent_StartAtAsc(userId);
        return eventsParticipant
                .stream()
                .map(EventParticipant::getEvent)
                .filter(Objects::nonNull)
                .distinct()
                .map(event -> {
                    return new EventDetailDto(
                            event.getId(),
                            event.getMusicProject().getId(),
                            event.getTitle(),                 // title
                            event.getDescription(),
                            event.getStartAt().toLocalDate(),// LocalDate
                            Time.valueOf(event.getStartAt().toLocalTime()), // java.sql.Time
                            event.getLocation(),
                            event.getMusicProject().getName(),
                            event.getMusicProject().getProfileImage()
                    );
                })
                .toList();
    }

    @Override
    public EventDetailDto getEventById(UUID eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ValidationException("Evento não encontrado."));
        return eventMapper.toDetailDto(event);
    }
}
