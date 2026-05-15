package br.com.louvor4.api.strategy.event;

import br.com.louvor4.api.enums.SetlistItemType;
import br.com.louvor4.api.models.EventParticipant;
import br.com.louvor4.api.models.EventSetlistItem;

import java.util.UUID;

public interface EventSetlistItemStrategy {

    SetlistItemType getType();

    EventSetlistItem create(UUID itemId, EventParticipant participant, Integer sequence);
}
