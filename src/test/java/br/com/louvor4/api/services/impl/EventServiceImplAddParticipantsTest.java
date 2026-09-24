package br.com.louvor4.api.services.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.enums.ProjectMemberStatus;
import br.com.louvor4.api.exceptions.ValidationException;
import br.com.louvor4.api.mapper.EventMapper;
import br.com.louvor4.api.mapper.EventSetlistItemMapper;
import br.com.louvor4.api.models.Event;
import br.com.louvor4.api.models.EventParticipant;
import br.com.louvor4.api.models.MusicProject;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceImplAddParticipantsTest {

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
    private Event event;
    private MusicProject project;

    @BeforeEach
    void setUp() {
        project = project();
        eventId = UUID.randomUUID();
        event = new Event();
        event.setId(eventId);
        event.setMusicProject(project);
        lenient().when(eventRepository.findById(eventId)).thenReturn(Optional.of(event));
    }

    private MusicProject project() {
        MusicProject p = new MusicProject();
        p.setId(UUID.randomUUID());
        return p;
    }

    private MusicProjectMember member(ProjectMemberStatus status) {
        return member(status, project);
    }

    private MusicProjectMember member(ProjectMemberStatus status, MusicProject memberProject) {
        MusicProjectMember member = new MusicProjectMember();
        member.setId(UUID.randomUUID());
        member.setStatus(status);
        member.setMusicProject(memberProject);
        return member;
    }

    @Test
    void addingNewParticipant_rejectsMemberFromAnotherProject() {
        MusicProjectMember outsider = member(ProjectMemberStatus.ACTIVE, project());
        when(eventParticipantRepository.findByEventId(eventId)).thenReturn(List.of());
        when(musicProjectMemberRepository.findById(outsider.getId())).thenReturn(Optional.of(outsider));

        assertThatThrownBy(() -> service.addOrUpdateParticipantsToEvent(eventId, List.of(dtoFor(outsider))))
                .isInstanceOf(ValidationException.class)
                .hasMessage("Membro não encontrado");

        verify(eventParticipantRepository, never()).saveAll(any());
    }

    private EventParticipantDTO dtoFor(MusicProjectMember member) {
        EventParticipantDTO dto = new EventParticipantDTO();
        dto.setMemberId(member.getId());
        return dto;
    }

    @ParameterizedTest
    @EnumSource(value = ProjectMemberStatus.class, names = {"PENDING_INVITE", "REMOVED", "DECLINED"})
    void addingNewParticipant_rejectsMemberThatIsNotActive(ProjectMemberStatus status) {
        MusicProjectMember member = member(status);
        when(eventParticipantRepository.findByEventId(eventId)).thenReturn(List.of());
        when(musicProjectMemberRepository.findById(member.getId())).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> service.addOrUpdateParticipantsToEvent(eventId, List.of(dtoFor(member))))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("membros ativos");

        verify(eventParticipantRepository, never()).saveAll(any());
    }

    @Test
    void updatingExistingParticipant_keepsItEvenWhenMemberIsNoLongerActive() {
        MusicProjectMember member = member(ProjectMemberStatus.REMOVED);
        EventParticipant existing = new EventParticipant();
        existing.setId(UUID.randomUUID());
        existing.setEvent(event);
        existing.setMember(member);
        when(eventParticipantRepository.findByEventId(eventId)).thenReturn(List.of(existing));

        assertThatNoException().isThrownBy(
                () -> service.addOrUpdateParticipantsToEvent(eventId, List.of(dtoFor(member))));
    }
}
