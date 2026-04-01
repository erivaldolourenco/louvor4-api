package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.EventSong;
import br.com.louvor4.api.repositories.projections.EventCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EventSongRepository extends JpaRepository<EventSong, UUID> {
    List<EventSong> getEventSongByEventId(UUID eventId);
    List<EventSong> findByEventIdIn(List<UUID> eventIds);
    boolean existsByEventIdAndSongId(UUID eventId, UUID songId);
    void deleteByAddedBy_IdIn(List<UUID> participantIds);

    @Query("""
            select es.event.id as eventId, count(distinct es.song.id) as total
            from EventSong es
            where es.event.id in :eventIds
            group by es.event.id
            """)
    List<EventCountProjection> countDistinctSongsByEventIds(@Param("eventIds") List<UUID> eventIds);
}
