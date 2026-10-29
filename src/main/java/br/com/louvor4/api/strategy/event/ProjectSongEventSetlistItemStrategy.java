package br.com.louvor4.api.strategy.event;

import br.com.louvor4.api.enums.SetlistItemType;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.models.EventParticipant;
import br.com.louvor4.api.models.EventSetlistItem;
import br.com.louvor4.api.models.ProjectSong;
import br.com.louvor4.api.repositories.ProjectSongRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ProjectSongEventSetlistItemStrategy implements EventSetlistItemStrategy {

    private final ProjectSongRepository projectSongRepository;

    public ProjectSongEventSetlistItemStrategy(ProjectSongRepository projectSongRepository) {
        this.projectSongRepository = projectSongRepository;
    }

    @Override
    public SetlistItemType getType() {
        return SetlistItemType.PROJECT_SONG;
    }

    // Só aceita música do mesmo projeto do evento e que não foi excluída.
    @Override
    public EventSetlistItem create(UUID itemId, EventParticipant participant, Integer sequence) {
        UUID projectId = participant.getEvent().getMusicProject().getId();

        ProjectSong song = projectSongRepository.findByIdAndMusicProject_IdAndDeletedAtIsNull(itemId, projectId)
                .orElseThrow(() ->
                        new NotFoundException("Música do projeto não encontrada: " + itemId));

        EventSetlistItem item = new EventSetlistItem();
        item.setEvent(participant.getEvent());
        item.setType(SetlistItemType.PROJECT_SONG);
        item.setSequence(sequence);
        item.setProjectSong(song);
        item.setAddedBy(participant);

        return item;
    }
}
