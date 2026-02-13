package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.EventPermission;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.mapper.EventMapper;
import br.com.louvor4.api.mapper.EventSongMapper;
import br.com.louvor4.api.models.*;
import br.com.louvor4.api.repositories.*;
import br.com.louvor4.api.services.EventService;
import br.com.louvor4.api.services.PushSenderService;
import br.com.louvor4.api.shared.dto.Event.EventDetailDto;
import br.com.louvor4.api.shared.dto.Event.EventParticipantDTO;
import br.com.louvor4.api.shared.dto.Event.EventParticipantResponseDTO;
import br.com.louvor4.api.shared.dto.Song.AddEventSongDTO;
import br.com.louvor4.api.shared.dto.Song.EventSongDTO;
import br.com.louvor4.api.shared.dto.Song.SongDTO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final MusicProjectMemberRepository musicProjectMemberRepository;
    private final EventMapper eventMapper;
    private final EventSongMapper eventSongMapper;
    private final CurrentUserProvider currentUserProvider;
    private final ProjectSkillRepository projectSkillRepository;
    private final SongRepository songRepository;
    private final EventSongRepository eventSongRepository;
    private final PushSenderService senderService;



    public EventServiceImpl(
            EventRepository eventRepository,
            EventParticipantRepository eventParticipantRepository,
            MusicProjectMemberRepository musicProjectMemberRepository, EventMapper eventMapper, EventSongMapper eventSongMapper, CurrentUserProvider currentUserProvider, ProjectSkillRepository projectSkillRepository, SongRepository songRepository, EventSongRepository eventSongRepository, PushSenderService senderService
    ) {
        this.eventRepository = eventRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.musicProjectMemberRepository = musicProjectMemberRepository;
        this.eventMapper = eventMapper;
        this.eventSongMapper = eventSongMapper;
        this.currentUserProvider = currentUserProvider;
        this.projectSkillRepository = projectSkillRepository;
        this.songRepository = songRepository;
        this.eventSongRepository = eventSongRepository;
        this.senderService = senderService;
    }

    @Transactional
    @Override
    public void addOrUpdateParticipantsToEvent(UUID eventId, List<EventParticipantDTO> participantsDto) {
        Event event = findEventOrThrow(eventId);
        List<EventParticipantDTO> incoming = (participantsDto == null) ? List.of() : participantsDto;

        validateRequestAllowEmpty(incoming);
        List<EventParticipant> currentList = eventParticipantRepository.findByEventId(eventId);

        if (incoming.isEmpty()) {
            if (!currentList.isEmpty()) eventParticipantRepository.deleteAllInBatch(currentList);
            return;
        }

        Map<UUID, EventParticipant> currentByMemberId = currentList.stream()
                .collect(Collectors.toMap(p -> p.getMember().getId(), p -> p));

        List<EventParticipant> toSave = new ArrayList<>(incoming.size());
        Set<UUID> incomingMemberIds = new HashSet<>(incoming.size());

        // Lista para rastrear quem deve receber a notificação (apenas os novos)
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
                EventParticipant newParticipant = createParticipant(event, member, skill, perms);
                toSave.add(newParticipant);
                newParticipants.add(newParticipant); // Guardamos para notificar depois
            }
        }

        // Limpeza de removidos
        List<EventParticipant> toDelete = currentList.stream()
                .filter(p -> !incomingMemberIds.contains(p.getMember().getId()))
                .toList();
        if (!toDelete.isEmpty()) eventParticipantRepository.deleteAllInBatch(toDelete);

        // Salva no banco primeiro
        eventParticipantRepository.saveAll(toSave);

        // Envia as notificações para os novos integrantes
        notifyNewParticipants(event, newParticipants);
    }

    private void notifyNewParticipants(Event event, List<EventParticipant> newParticipants) {
        String title = "Nova Escala: " + event.getTitle();
        String message = "Você foi escalado para o evento do dia " + event.getStartAt().toString();

        for (EventParticipant participant : newParticipants) {
            try {
                // Supondo que o MusicProjectMember tenha um vínculo com o User (UUID userId)
                UUID userId = participant.getMember().getUser().getId();
                senderService.sendToUser(userId, title, message);
            } catch (Exception e) {
                // Logar o erro mas não travar a execução principal
                System.err.println("Falha ao enviar push para usuário: " + e.getMessage());
            }
        }
    }

    private void validateRequestAllowEmpty(List<EventParticipantDTO> participants) {
        if (participants == null) return; // vai virar vazio antes, mas ok

        if (participants.isEmpty()) return;

        Set<UUID> seen = new HashSet<>();
        for (EventParticipantDTO dto : participants) {
            if (dto.getMemberId() == null) {
                throw new ValidationException("memberId é obrigatório.");
            }
            if (!seen.add(dto.getMemberId())) {
                throw new ValidationException("Participante duplicado no payload: " + dto.getMemberId());
            }
        }
    }
    private Event findEventOrThrow(UUID eventId) {
        if (eventId == null) {
            throw new ValidationException("Id do evento é obrigatório.");
        }

        return eventRepository.findById(eventId)
                .orElseThrow(() ->
                        new ValidationException("Evento não encontrado.")
                );
    }


    private void validateRequest(List<EventParticipantDTO> participantsDto) {
        if (participantsDto == null) {
            throw new ValidationException("Lista de participantes não pode ser nula.");
        }

        Set<UUID> seen = new HashSet<>();
        for (EventParticipantDTO dto : participantsDto) {
            if (dto == null || dto.getMemberId() == null) {
                throw new ValidationException("Participante inválido: memberId é obrigatório.");
            }
            if (!seen.add(dto.getMemberId())) {
                throw new ValidationException("Participante duplicado no request: " + dto.getMemberId());
            }
        }
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
        return participant;
    }


    @Override
    public List<EventDetailDto> getEventsByUser() {
        UUID userId = currentUserProvider.get().getId();

        List<EventParticipant> eventsParticipant = eventParticipantRepository
                .findByMember_User_IdAndEvent_StartAtGreaterThanEqualOrderByEvent_StartAtAsc(userId, LocalDateTime.now().minusDays(1));

        return eventsParticipant
                .stream()
                .map(EventParticipant::getEvent)
                .filter(Objects::nonNull)
                .distinct()
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
                        p.getMember().getId(),
                        p.getMember().getUser().getFirstName(),
                        p.getMember().getUser().getLastName(),
                        p.getMember().getUser().getProfileImage(),
                        p.getSkill() != null ? p.getSkill().getId() : null,
                        p.getPermissions()
                ))
                .toList();
    }

    @Override
    public void addSongToEvent(UUID eventId, AddEventSongDTO addEventSongDto) {
        User user = currentUserProvider.get();

        EventParticipant participant = eventParticipantRepository
                .findByEventIdAndMemberUserId(eventId, user.getId())
                .orElseThrow(() -> new ValidationException("Usuário não está escalado como participante deste evento."));

        if (!participant.getPermissions().contains(EventPermission.ADD_SONG)) {
            throw new ValidationException(
                    "Você não tem permissão para adicionar músicas neste evento.");
        }

        Song song = songRepository.findById(addEventSongDto.songId())
                .orElseThrow(() -> new NotFoundException( "Música não encontrada."));

        EventSong eventSong = new EventSong();
        eventSong.setEvent(participant.getEvent());
        eventSong.setSong(song);
        eventSong.setAddedBy(participant);

        eventSongRepository.save(eventSong);
    }

    @Override
    public List<EventSongDTO> getEventSongs(UUID eventId) {
        List<EventSong> eventSongs = eventSongRepository.getEventSongByEventId(eventId);
        return eventSongMapper.toSongDtoList(eventSongs);
    }

    @Override
    @Transactional
    public void removeSongFromEvent(UUID eventId, UUID eventSongId) {
        if (eventId == null) {
            throw new ValidationException("Id do evento é obrigatório.");
        }
        if (eventSongId == null) {
            throw new ValidationException("Id da música do evento é obrigatório.");
        }
        User user = currentUserProvider.get();
        EventParticipant participant = eventParticipantRepository
                .findByEventIdAndMemberUserId(eventId, user.getId())
                .orElseThrow(() -> new ValidationException("Usuário não está escalado como participante deste evento."));

        if (!participant.getPermissions().contains(EventPermission.ADD_SONG)) {
            throw new ValidationException("Você não tem permissão para remover músicas neste evento.");
        }

        EventSong eventSong = eventSongRepository.findById(eventSongId)
                .orElseThrow(() -> new NotFoundException("Música não encontrada no evento."));

        if (eventSong.getEvent() == null || !eventId.equals(eventSong.getEvent().getId())) {
            throw new ValidationException("Esta música não pertence ao evento informado.");
        }

        eventSongRepository.delete(eventSong);
    }

    @Override
    public void deleteEventById(UUID id) {

        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ValidationException("Evento não encontrado."));
        Integer participants = eventRepository.countParticipantsByEventId(id);
        if (participants != null && participants > 0) {
            throw new ValidationException("Não é possível excluir o evento: existem participantes associados.");
        }

        Integer songs = eventRepository.countSongsByEventId(id);
        if (songs != null && songs > 0) {
            throw new ValidationException("Não é possível excluir o evento: existem músicas associadas.");
        }

        eventRepository.delete(event);
    }
}
