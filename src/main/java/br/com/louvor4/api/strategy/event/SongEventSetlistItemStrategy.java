package br.com.louvor4.api.strategy.event;

import br.com.louvor4.api.enums.SetlistItemType;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.models.EventParticipant;
import br.com.louvor4.api.models.EventSetlistItem;
import br.com.louvor4.api.models.Song;
import br.com.louvor4.api.repositories.SongRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class SongEventSetlistItemStrategy implements EventSetlistItemStrategy {

    private final SongRepository songRepository;

    public SongEventSetlistItemStrategy(SongRepository songRepository) {
        this.songRepository = songRepository;
    }

    @Override
    public SetlistItemType getType() {
        return SetlistItemType.SONG;
    }

    @Override
    public EventSetlistItem create(UUID itemId, EventParticipant participant, Integer sequence) {

        Song song = songRepository.findById(itemId)
                .orElseThrow(() ->
                        new NotFoundException("Música não encontrada: " + itemId));

        EventSetlistItem item = new EventSetlistItem();
        item.setEvent(participant.getEvent());
        item.setType(SetlistItemType.SONG);
        item.setSequence(sequence);
        item.setSong(song);
        item.setAddedBy(participant);

        return item;
    }
}
