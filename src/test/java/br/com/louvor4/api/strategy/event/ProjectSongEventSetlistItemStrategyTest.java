package br.com.louvor4.api.strategy.event;

import br.com.louvor4.api.enums.SetlistItemType;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.models.Event;
import br.com.louvor4.api.models.EventParticipant;
import br.com.louvor4.api.models.EventSetlistItem;
import br.com.louvor4.api.models.MusicProject;
import br.com.louvor4.api.models.ProjectSong;
import br.com.louvor4.api.repositories.ProjectSongRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProjectSongEventSetlistItemStrategyTest {

    @Mock ProjectSongRepository projectSongRepository;

    @InjectMocks ProjectSongEventSetlistItemStrategy strategy;

    private UUID projectId;
    private EventParticipant participant;

    @BeforeEach
    void setUp() {
        projectId = UUID.randomUUID();
        MusicProject project = new MusicProject();
        project.setId(projectId);
        Event event = new Event();
        event.setMusicProject(project);
        participant = new EventParticipant();
        participant.setEvent(event);
    }

    @Test
    void createShouldBuildProjectSongItemFromEventProject() {
        UUID songId = UUID.randomUUID();
        ProjectSong song = new ProjectSong();
        song.setId(songId);
        when(projectSongRepository.findByIdAndMusicProject_IdAndDeletedAtIsNull(songId, projectId))
                .thenReturn(Optional.of(song));

        EventSetlistItem item = strategy.create(songId, participant, 3);

        assertThat(item.getType()).isEqualTo(SetlistItemType.PROJECT_SONG);
        assertThat(item.getProjectSong()).isSameAs(song);
        assertThat(item.getSong()).isNull();
        assertThat(item.getSongData()).isSameAs(song);
        assertThat(item.getSequence()).isEqualTo(3);
        assertThat(item.getAddedBy()).isSameAs(participant);
    }

    // A busca já filtra pelo projeto do evento e por não excluída: música de outro
    // projeto ou excluída cai aqui.
    @Test
    void createShouldRejectSongOutsideEventProjectOrDeleted() {
        UUID songId = UUID.randomUUID();
        when(projectSongRepository.findByIdAndMusicProject_IdAndDeletedAtIsNull(songId, projectId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> strategy.create(songId, participant, 1))
                .isInstanceOf(NotFoundException.class);
    }
}
