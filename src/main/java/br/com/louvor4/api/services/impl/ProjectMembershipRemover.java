package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.enums.ProjectMemberStatus;
import br.com.louvor4.api.models.EventParticipant;
import br.com.louvor4.api.models.MusicProjectMember;
import br.com.louvor4.api.repositories.EventParticipantRepository;
import br.com.louvor4.api.repositories.EventProgramItemRepository;
import br.com.louvor4.api.repositories.EventSetlistItemRepository;
import br.com.louvor4.api.repositories.MusicProjectMemberRepository;
import br.com.louvor4.api.repositories.ProjectResponsibilityAssignmentRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Tira um membro de um projeto: remove as participações dele em eventos futuros e marca o
 * vínculo como REMOVED. Participações em eventos passados são preservadas como histórico.
 */
@Component
public class ProjectMembershipRemover {

    private final MusicProjectMemberRepository musicProjectMemberRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final EventSetlistItemRepository eventSetlistItemRepository;
    private final EventProgramItemRepository eventProgramItemRepository;
    private final ProjectResponsibilityAssignmentRepository responsibilityAssignmentRepository;

    public ProjectMembershipRemover(MusicProjectMemberRepository musicProjectMemberRepository,
                                    EventParticipantRepository eventParticipantRepository,
                                    EventSetlistItemRepository eventSetlistItemRepository,
                                    EventProgramItemRepository eventProgramItemRepository,
                                    ProjectResponsibilityAssignmentRepository responsibilityAssignmentRepository) {
        this.musicProjectMemberRepository = musicProjectMemberRepository;
        this.eventParticipantRepository = eventParticipantRepository;
        this.eventSetlistItemRepository = eventSetlistItemRepository;
        this.eventProgramItemRepository = eventProgramItemRepository;
        this.responsibilityAssignmentRepository = responsibilityAssignmentRepository;
    }

    public void remove(MusicProjectMember member) {
        LocalDateTime now = LocalDateTime.now();
        List<EventParticipant> futureParticipants = eventParticipantRepository
                .findByMember_IdAndEvent_StartAtGreaterThan(member.getId(), now);
        removeFutureParticipations(futureParticipants, now);
        // Responsabilidades: períodos que ainda não começaram saem; o atual e os passados ficam no histórico
        responsibilityAssignmentRepository.deleteNotStartedByMemberId(member.getId(), LocalDate.now());

        member.setStatus(ProjectMemberStatus.REMOVED);
        musicProjectMemberRepository.save(member);
    }

    // Remove, de eventos futuros, o vínculo do membro: primeiro os itens do roteiro
    // (event_program_items) que apontam pro item do repertório, depois o próprio item
    // do repertório (event_setlist_items) e por fim a participação (event_participants).
    // A ordem importa por causa das FKs: event_program_items -> event_setlist_items -> event_participants.
    private void removeFutureParticipations(List<EventParticipant> futureParticipants, LocalDateTime now) {
        if (futureParticipants.isEmpty()) {
            return;
        }

        List<UUID> futureParticipantIds = futureParticipants.stream()
                .map(EventParticipant::getId)
                .toList();

        List<UUID> futureSetlistItemIds = eventSetlistItemRepository
                .findIdsByAddedBy_IdInAndEvent_StartAtGreaterThan(futureParticipantIds, now);
        if (!futureSetlistItemIds.isEmpty()) {
            eventProgramItemRepository.deleteBySetlistItemIdIn(futureSetlistItemIds);
        }

        eventSetlistItemRepository.deleteByAddedBy_IdInAndEvent_StartAtGreaterThan(futureParticipantIds, now);
        eventParticipantRepository.deleteAllInBatch(futureParticipants);
    }
}
