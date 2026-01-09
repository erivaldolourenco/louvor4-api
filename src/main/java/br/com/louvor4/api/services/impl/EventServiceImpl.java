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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
import java.util.*;

@Service
public class EventServiceImpl implements EventService {
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final MusicProjectMemberRepository musicProjectMemberRepository;
    private final EventMapper eventMapper;
    private final CurrentUserProvider currentUserProvider;
    private final ProjectSkillRepository projectSkillRepository;


    public EventServiceImpl(
            EventRepository eventRepository,
            UserRepository userRepository,
            EventParticipantRepository eventParticipantRepository,
            MusicProjectMemberRepository musicProjectMemberRepository, EventMapper eventMapper, CurrentUserProvider currentUserProvider, ProjectSkillRepository projectSkillRepository
    ) {
        this.eventRepository = eventRepository;
        this.userRepository = userRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.musicProjectMemberRepository = musicProjectMemberRepository;
        this.eventMapper = eventMapper;
        this.currentUserProvider = currentUserProvider;
        this.projectSkillRepository = projectSkillRepository;
    }

    @Transactional
    @Override
    public void addParticipantToEvent(UUID eventId, EventParticipantDTO participantDto) {
        Event event = findEventOrThrow(eventId);

        MusicProjectMember member = musicProjectMemberRepository
                .findByMusicProject_IdAndUser_Id(event.getMusicProject().getId(), participantDto.getUserId())
                .orElseThrow(() -> new ValidationException("O usuário não é membro deste projeto."));

        // 3. Recuperar e Validar a Skill (se fornecida)
        ProjectSkill skill = null;
        if (participantDto.getSkillId() != null) {
            skill = validateAndGetSkill(member, participantDto.getSkillId());
        }

        // 4. Verificar Duplicidade (Agora considerando a Skill para permitir multi-instrumentismo no mesmo evento)
        validateDuplicateParticipant(event.getId(), member.getId(), participantDto.getSkillId());

        // 5. Mapear e Salvar
        EventParticipant participant = createParticipant(event, member, skill, participantDto.getPermissions());
        eventParticipantRepository.save(participant);
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
}
