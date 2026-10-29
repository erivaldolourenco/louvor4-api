package br.com.louvor4.api.mapper;

import br.com.louvor4.api.enums.SetlistItemType;
import br.com.louvor4.api.models.EventSetlistItem;
import br.com.louvor4.api.models.ProjectSong;
import br.com.louvor4.api.shared.dto.Event.PublicSetlistItemDTO;
import br.com.louvor4.api.shared.dto.Event.SetlistDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventSetlistItemMapperProjectSongTest {

    private final EventSetlistItemMapper mapper = new EventSetlistItemMapperImpl();

    private ProjectSong song;
    private EventSetlistItem item;

    @BeforeEach
    void setUp() {
        song = new ProjectSong();
        song.setId(UUID.randomUUID());
        song.setTitle("Ressuscita-me");
        song.setArtist("Aline Barros");
        song.setKey("G");
        song.setBpm(72);
        song.setNotes("Entrada suave");

        item = new EventSetlistItem();
        item.setId(UUID.randomUUID());
        item.setType(SetlistItemType.PROJECT_SONG);
        item.setProjectSong(song);
    }

    @Test
    void setlistShouldExposeProjectSongAsEventSong() {
        SetlistDTO dto = mapper.toSetlistDto(item);

        assertThat(dto.type()).isEqualTo(SetlistItemType.PROJECT_SONG);
        assertThat(dto.eventMedley()).isNull();
        assertThat(dto.eventSong()).isNotNull();
        assertThat(dto.eventSong().songId()).isEqualTo(song.getId());
        assertThat(dto.eventSong().title()).isEqualTo("Ressuscita-me");
        assertThat(dto.eventSong().key()).isEqualTo("G");
        assertThat(dto.notes()).isEqualTo("Entrada suave");
    }

    @Test
    void setlistShouldPreferEventKeyOverSongKey() {
        item.setKey("A");

        assertThat(mapper.toSetlistDto(item).eventSong().key()).isEqualTo("A");
    }

    @Test
    void publicSetlistShouldIncludeProjectSong() {
        PublicSetlistItemDTO dto = mapper.toPublicSetlistItemDto(item);

        assertThat(dto.song()).isNotNull();
        assertThat(dto.song().title()).isEqualTo("Ressuscita-me");
        assertThat(dto.song().bpm()).isEqualTo(72);
    }

    @Test
    void monthOverviewShouldIncludeProjectSong() {
        assertThat(mapper.toMonthOverviewSong(item).songId()).isEqualTo(song.getId());
    }
}
