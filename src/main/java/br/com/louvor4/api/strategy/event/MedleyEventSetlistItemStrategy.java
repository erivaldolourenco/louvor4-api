package br.com.louvor4.api.strategy.event;

import br.com.louvor4.api.enums.SetlistItemType;
import br.com.louvor4.api.exceptions.NotFoundException;
import br.com.louvor4.api.models.EventParticipant;
import br.com.louvor4.api.models.EventSetlistItem;
import br.com.louvor4.api.models.Medley;
import br.com.louvor4.api.repositories.MedleyRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MedleyEventSetlistItemStrategy implements EventSetlistItemStrategy{
    private final MedleyRepository medleyRepository;

    public MedleyEventSetlistItemStrategy(MedleyRepository medleyRepository) {
        this.medleyRepository = medleyRepository;
    }

    @Override
    public SetlistItemType getType() {
        return SetlistItemType.MEDLEY;
    }

    @Override
    public EventSetlistItem create(UUID itemId, EventParticipant participant, Integer sequence) {

        Medley medley = medleyRepository.findMedleyById(itemId).orElseThrow(() ->
                new NotFoundException("Medley não encontrada: " + itemId));
        EventSetlistItem item = new EventSetlistItem();
        item.setEvent(participant.getEvent());
        item.setType(SetlistItemType.MEDLEY);
        item.setSequence(sequence);
        item.setMedley(medley);
        item.setAddedBy(participant);

        return item;
    }
}
