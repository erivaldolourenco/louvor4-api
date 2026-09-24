package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.mapper.EventMapper;
import br.com.louvor4.api.mapper.EventSetlistItemMapper;
import br.com.louvor4.api.models.Event;
import br.com.louvor4.api.repositories.*;
import br.com.louvor4.api.services.EventReminderScheduler;
import br.com.louvor4.api.services.PushSenderService;
import br.com.louvor4.api.services.ProgramService;
import br.com.louvor4.api.services.UserNotificationService;
import br.com.louvor4.api.strategy.event.EventSetlistItemStrategyResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplDeleteEventTest {

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
    @Mock PushSenderService senderService;
    @Mock UserNotificationService userNotificationService;
    @Mock UserUnavailabilityRepository userUnavailabilityRepository;
    @Mock EventSetlistItemStrategyResolver strategyResolver;
    @Mock ProgramService programService;
    @Mock EventReminderScheduler eventReminderScheduler;
    @Mock AudioFileRepository audioFileRepository;
    @InjectMocks EventServiceImpl service;

    @Test
    void deleteEventById_deletesEventWithoutValidatingParticipantsOrSongs() {
        UUID eventId = UUID.randomUUID();
        Event event = new Event();
        event.setId(eventId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        assertThatNoException().isThrownBy(() -> service.deleteEventById(eventId));

        verify(eventRepository).delete(event);
        verify(eventRepository, never()).countParticipantsByEventId(any());
        verify(eventRepository, never()).countSongsByEventId(any(), any());
    }

    @Test
    void deleteEventById_deletesChildEntitiesInCorrectOrderBeforeEvent() {
        UUID eventId = UUID.randomUUID();
        Event event = new Event();
        event.setId(eventId);
        when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));

        service.deleteEventById(eventId);

        InOrder inOrder = inOrder(
                eventProgramItemRepository,
                eventSetlistItemRepository,
                eventParticipantRepository,
                eventReminderScheduler,
                eventRepository
        );
        inOrder.verify(eventParticipantRepository).deleteByEventId(eventId);
        inOrder.verify(eventReminderScheduler).cancel(eventId);
        inOrder.verify(eventRepository).delete(event);
    }

    @Test
    void deleteEventById_throwsWhenEventNotFound() {
        UUID eventId = UUID.randomUUID();
        when(eventRepository.findById(eventId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deleteEventById(eventId))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Evento não encontrado.");
    }
}
