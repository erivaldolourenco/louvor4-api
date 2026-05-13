package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.EventParticipantStatus;
import br.com.louvor4.api.enums.SetlistItemType;
import br.com.louvor4.api.mapper.EventMapper;
import br.com.louvor4.api.mapper.EventSetlistItemMapper;
import br.com.louvor4.api.models.*;
import br.com.louvor4.api.repositories.*;
import br.com.louvor4.api.services.EventReminderScheduler;
import br.com.louvor4.api.services.PushSenderService;
import br.com.louvor4.api.services.ProgramService;
import br.com.louvor4.api.services.UserNotificationService;
import br.com.louvor4.api.shared.dto.Event.UserEventDetailDto;
import br.com.louvor4.api.strategy.event.EventSetlistItemStrategyResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplGetPastEventsTest {

    @Mock EventRepository eventRepository;
    @Mock EventParticipantRepository eventParticipantRepository;
    @Mock MusicProjectMemberRepository musicProjectMemberRepository;
    @Mock EventMapper eventMapper;
    @Mock EventSetlistItemMapper eventSetlistItemMapper;
    @Mock CurrentUserProvider currentUserProvider;
    @Mock ProjectSkillRepository projectSkillRepository;
    @Mock SongRepository songRepository;
    @Mock EventSetlistItemRepository eventSetlistItemRepository;
    @Mock PushSenderService senderService;
    @Mock UserNotificationService userNotificationService;
    @Mock UserUnavailabilityRepository userUnavailabilityRepository;
    @Mock EventSetlistItemStrategyResolver strategyResolver;
    @Mock ProgramService programService;
    @Mock EventReminderScheduler eventReminderScheduler;
    @InjectMocks EventServiceImpl service;

    private UUID userId;
    private Pageable pageable;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "startAt"));
        User user = new User();
        user.setId(userId);
        when(currentUserProvider.get()).thenReturn(user);
    }

    @Test
    void getPastEventsByUser_returnsEmptyPageWhenNoParticipants() {
        when(eventParticipantRepository.findPastByUserWithEventAndProjectAndMemberUser(
                eq(userId), eq(EventParticipantStatus.ACCEPTED), any(LocalDateTime.class), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        Page<UserEventDetailDto> result = service.getPastEventsByUser(pageable);

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

    @Test
    void getPastEventsByUser_returnsDtosForEachEvent() {
        MusicProject project = new MusicProject();
        project.setId(UUID.randomUUID());
        project.setName("Louvor");

        Event event = new Event();
        event.setId(UUID.randomUUID());
        event.setTitle("Culto Passado");
        event.setStartAt(LocalDateTime.now().minusDays(7));
        event.setMusicProject(project);

        EventParticipant participant = new EventParticipant();
        participant.setEvent(event);
        participant.setMember(new MusicProjectMember());
        participant.setStatus(EventParticipantStatus.ACCEPTED);

        Page<EventParticipant> repoPage = new PageImpl<>(List.of(participant), pageable, 1);
        when(eventParticipantRepository.findPastByUserWithEventAndProjectAndMemberUser(
                eq(userId), eq(EventParticipantStatus.ACCEPTED), any(LocalDateTime.class), eq(pageable)))
                .thenReturn(repoPage);
        when(eventParticipantRepository.countDistinctMembersByEventIds(any())).thenReturn(List.of());
        when(eventParticipantRepository.findProfileImagesByEventIds(any())).thenReturn(List.of());
        when(eventSetlistItemRepository.countDistinctSongsByEventIds(any(), eq(SetlistItemType.SONG)))
                .thenReturn(List.of());

        Page<UserEventDetailDto> result = service.getPastEventsByUser(pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).title()).isEqualTo("Culto Passado");
        assertThat(result.getTotalElements()).isEqualTo(1);
    }

    @Test
    void getPastEventsByUser_passesAcceptedStatusAndCurrentTimeToQuery() {
        when(eventParticipantRepository.findPastByUserWithEventAndProjectAndMemberUser(
                eq(userId), eq(EventParticipantStatus.ACCEPTED), any(LocalDateTime.class), eq(pageable)))
                .thenReturn(Page.empty(pageable));

        service.getPastEventsByUser(pageable);

        verify(eventParticipantRepository).findPastByUserWithEventAndProjectAndMemberUser(
                eq(userId),
                eq(EventParticipantStatus.ACCEPTED),
                any(LocalDateTime.class),
                eq(pageable));
    }
}
