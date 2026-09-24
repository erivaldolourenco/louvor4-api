package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.mapper.EventMapper;
import br.com.louvor4.api.mapper.EventSetlistItemMapper;
import br.com.louvor4.api.models.Event;
import br.com.louvor4.api.models.EventParticipant;
import br.com.louvor4.api.models.MusicProjectMember;
import br.com.louvor4.api.repositories.*;
import br.com.louvor4.api.services.EventReminderScheduler;
import br.com.louvor4.api.services.ProgramService;
import br.com.louvor4.api.services.UserNotificationService;
import br.com.louvor4.api.shared.dto.Event.EventParticipantDTO;
import br.com.louvor4.api.strategy.event.EventSetlistItemStrategyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplRemoveParticipantsTest {

    @Mock EventRepository eventRepository;
    @Mock EventParticipantRepository eventParticipantRepository;
    @Mock EventSetlistItemRepository eventSetlistItemRepository;
    @Mock EventProgramItemRepository eventProgramItemRepository;
    @Mock MusicProjectMemberRepository musicProjectMemberRepository;
    @Mock EventMapper eventMapper;
    @Mock EventSetlistItemMapper eventSetlistItemMapper;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock ProjectSkillRepository projectSkillRepository;
    @Mock SongRepository songRepository;
    @Mock UserNotificationService userNotificationService;
    @Mock UserUnavailabilityRepository userUnavailabilityRepository;
    @Mock EventSetlistItemStrategyResolver strategyResolver;
    @Mock ProgramService programService;
    @Mock EventReminderScheduler eventReminderScheduler;
    @Mock AudioFileRepository audioFileRepository;
    @InjectMocks EventServiceImpl service;

    private UUID eventId;
    private EventParticipant kept;
    private EventParticipant removed;

    @BeforeEach
    void setUp() {
        eventId = UUID.randomUUID();
        Event event = new Event();
        event.setId(eventId);
        lenient().when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        kept = participant(event);
        removed = participant(event);
        lenient().when(eventParticipantRepository.findByEventId(eventId)).thenReturn(List.of(kept, removed));
    }

    @Test
    void removingParticipant_deletesTheirProgramAndSetlistItemsBeforeSoftDeletingThem() {
        UUID setlistItemId = UUID.randomUUID();
        when(eventSetlistItemRepository.findIdsByAddedBy_IdIn(List.of(removed.getId())))
                .thenReturn(List.of(setlistItemId));

        service.addOrUpdateParticipantsToEvent(eventId, List.of(dtoFor(kept)));

        InOrder inOrder = inOrder(eventProgramItemRepository, eventSetlistItemRepository, eventParticipantRepository);
        inOrder.verify(eventProgramItemRepository).deleteBySetlistItemIdIn(List.of(setlistItemId));
        inOrder.verify(eventSetlistItemRepository).deleteByAddedBy_IdIn(List.of(removed.getId()));
        inOrder.verify(eventParticipantRepository).softDeleteByIds(List.of(removed.getId()));
    }

    @Test
    void removingParticipant_withoutSetlistItems_stillSoftDeletesParticipant() {
        when(eventSetlistItemRepository.findIdsByAddedBy_IdIn(List.of(removed.getId())))
                .thenReturn(List.of());

        service.addOrUpdateParticipantsToEvent(eventId, List.of(dtoFor(kept)));

        verify(eventProgramItemRepository, never()).deleteBySetlistItemIdIn(any());
        verify(eventParticipantRepository).softDeleteByIds(List.of(removed.getId()));
    }

    @Test
    void removingAllParticipants_deletesTheirProgramAndSetlistItemsBeforeSoftDeletingThem() {
        UUID setlistItemId = UUID.randomUUID();
        List<UUID> allIds = List.of(kept.getId(), removed.getId());
        when(eventSetlistItemRepository.findIdsByAddedBy_IdIn(allIds)).thenReturn(List.of(setlistItemId));

        service.addOrUpdateParticipantsToEvent(eventId, List.of());

        InOrder inOrder = inOrder(eventProgramItemRepository, eventSetlistItemRepository, eventParticipantRepository);
        inOrder.verify(eventProgramItemRepository).deleteBySetlistItemIdIn(List.of(setlistItemId));
        inOrder.verify(eventSetlistItemRepository).deleteByAddedBy_IdIn(allIds);
        inOrder.verify(eventParticipantRepository).softDeleteByIds(allIds);
    }

    @Test
    void keepingEveryParticipant_doesNotTouchSetlistOrProgram() {
        service.addOrUpdateParticipantsToEvent(eventId, List.of(dtoFor(kept), dtoFor(removed)));

        verifyNoInteractions(eventProgramItemRepository);
        verify(eventSetlistItemRepository, never()).deleteByAddedBy_IdIn(any());
        verify(eventParticipantRepository, never()).softDeleteByIds(any());
    }

    private EventParticipant participant(Event event) {
        MusicProjectMember member = new MusicProjectMember();
        member.setId(UUID.randomUUID());

        EventParticipant participant = new EventParticipant();
        participant.setId(UUID.randomUUID());
        participant.setEvent(event);
        participant.setMember(member);
        return participant;
    }

    private EventParticipantDTO dtoFor(EventParticipant participant) {
        EventParticipantDTO dto = new EventParticipantDTO();
        dto.setMemberId(participant.getMember().getId());
        return dto;
    }
}
