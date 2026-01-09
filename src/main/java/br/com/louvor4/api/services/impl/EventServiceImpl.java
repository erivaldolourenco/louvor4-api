package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.EventPermission;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.mapper.EventMapper;
import br.com.louvor4.api.models.*;
import br.com.louvor4.api.repositories.*;
import br.com.louvor4.api.services.EventService;
import br.com.louvor4.api.shared.dto.Event.EventDetailDto;
import br.com.louvor4.api.shared.dto.Event.EventParticipantDTO;
import br.com.louvor4.api.shared.dto.Event.EventParticipantResponseDTO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final MusicProjectMemberRepository musicProjectMemberRepository;
    private final EventMapper eventMapper;
    private final CurrentUserProvider currentUserProvider;
    private final ProjectSkillRepository projectSkillRepository;


    public EventServiceImpl(
            EventRepository eventRepository,
            EventParticipantRepository eventParticipantRepository,
            MusicProjectMemberRepository musicProjectMemberRepository, EventMapper eventMapper, CurrentUserProvider currentUserProvider, ProjectSkillRepository projectSkillRepository
    ) {
        this.eventRepository = eventRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.musicProjectMemberRepository = musicProjectMemberRepository;
        this.eventMapper = eventMapper;
        this.currentUserProvider = currentUserProvider;
        this.projectSkillRepository = projectSkillRepository;
    }

    @Transactional
    @Override
    public void addParticipantsToEvent(UUID eventId, List<EventParticipantDTO> participantsDto) {
        Event event = findEventOrThrow(eventId);

        // 1. Busca os participantes que JÁ ESTÃO no banco para este evento
        List<EventParticipant> currentParticipants = eventParticipantRepository.findByEventId(eventId);

        // 2. Preparamos uma lista para o que será salvo (novos ou atualizados)
        List<EventParticipant> toSave = new ArrayList<>();

        for (EventParticipantDTO dto : participantsDto) {
            // Tenta encontrar se este membro já está na lista atual do banco
            Optional<EventParticipant> existing = currentParticipants.stream()
                    .filter(p -> p.getMember().getId().equals(dto.getMemberId()))
                    .findFirst();

            if (existing.isPresent()) {
                // ATUALIZA: O membro já estava lá, então só atualizamos a Skill (função)
                EventParticipant participant = existing.get();

                ProjectSkill skill = null;
                if (dto.getSkillId() != null) {
                    skill = projectSkillRepository.findById(dto.getSkillId())
                            .orElseThrow(() -> new ValidationException("Skill não encontrada"));
                }

                participant.setSkill(skill);
                toSave.add(participant);

                // Remove da lista temporária para não ser deletado depois
                currentParticipants.remove(participant);
            } else {
                // CRIA NOVO: O membro não estava no evento ainda
                MusicProjectMember member = musicProjectMemberRepository.findById(dto.getMemberId())
                        .orElseThrow(() -> new ValidationException("Membro não encontrado"));

                ProjectSkill skill = null;
                if (dto.getSkillId() != null) {
                    skill = projectSkillRepository.findById(dto.getSkillId()).orElse(null);
                }

                EventParticipant newParticipant = new EventParticipant();
                newParticipant.setEvent(event);
                newParticipant.setMember(member);
                newParticipant.setSkill(skill);
                toSave.add(newParticipant);
            }
        }

        // 3. DELETA: Quem sobrou na 'currentParticipants' é porque foi removido no Angular
        if (!currentParticipants.isEmpty()) {
            eventParticipantRepository.deleteAll(currentParticipants);
        }

        // 4. PERSISTE: Salva os novos e as atualizações de uma vez
        eventParticipantRepository.saveAll(toSave);
    }

// --- Métodos Auxiliares (Clean Code) ---

    private Event findEventOrThrow(UUID eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ValidationException("Evento não encontrado."));
    }

    private ProjectSkill validateAndGetSkill(MusicProjectMember member, UUID skillId) {
        // Busca a skill e já valida se ela pertence ao projeto do membro
        ProjectSkill skill = projectSkillRepository.findById(skillId)
                .orElseThrow(() -> new ValidationException("Função (Skill) não encontrada."));

        // Regra de Ouro: O membro possui essa skill no perfil dele?
        if (!member.getProjectSkills().contains(skill)) {
            throw new ValidationException("O membro não possui aptidão para esta função.");
        }
        return skill;
    }

    private void validateDuplicateParticipant(UUID eventId, UUID memberId, UUID skillId) {
        boolean exists = eventParticipantRepository.existsByEventIdAndMemberIdAndSkillId(eventId, memberId, skillId);
        if (exists) {
            throw new ValidationException("Este membro já está escalado para esta função neste evento.");
        }
    }

    private EventParticipant createParticipant(Event event, MusicProjectMember member, ProjectSkill skill, Set<EventPermission> perms) {
        EventParticipant participant = new EventParticipant();
        participant.setEvent(event);
        participant.setMember(member);
        participant.setSkill(skill);
        participant.setPermissions(perms == null || perms.isEmpty()
                ? EnumSet.noneOf(EventPermission.class)
                : EnumSet.copyOf(perms));
        return participant;
    }

    @Override
    public List<EventDetailDto> getEventsByUser() {
        UUID userId = currentUserProvider.get().getId();

        List<EventParticipant> eventsParticipant = eventParticipantRepository
                .findByMember_User_IdOrderByEvent_StartAtAsc(userId);

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
                        Time.valueOf(event.getStartAt().toLocalTime()),
                        event.getLocation(),
                        event.getMusicProject().getName(),
                        event.getMusicProject().getProfileImage()
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
                        p.getSkill() != null ? p.getSkill().getId() : null
                ))
                .toList();
    }
}
