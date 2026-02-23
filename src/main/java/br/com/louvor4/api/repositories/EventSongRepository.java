package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.EventSong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventSongRepository extends JpaRepository<EventSong, UUID> {
    List<EventSong> getEventSongByEventId(UUID eventId);
    List<EventSong> findByEventIdIn(List<UUID> eventIds);
    void deleteByAddedBy_IdIn(List<UUID> participantIds);
}
