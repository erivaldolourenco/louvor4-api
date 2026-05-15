package br.com.louvor4.api.repositories;

import br.com.louvor4.api.enums.SetlistItemType;
import br.com.louvor4.api.models.EventSetlistItem;
import br.com.louvor4.api.repositories.projections.EventCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EventSetlistItemRepository extends JpaRepository<EventSetlistItem, UUID> {

    List<EventSetlistItem> findByEventIdOrderBySequenceAsc(UUID eventId);

    List<EventSetlistItem> findByEventIdAndTypeOrderBySequenceAsc(UUID eventId, SetlistItemType type);

    List<EventSetlistItem> findByEventIdInAndType(List<UUID> eventIds, SetlistItemType type);

    void deleteByAddedBy_IdIn(List<UUID> participantIds);

    @Query("""
            select coalesce(max(esi.sequence), 0)
            from EventSetlistItem esi
            where esi.event.id = :eventId
            """)
    Integer findMaxSequenceByEventId(@Param("eventId") UUID eventId);

    @Query("""
            select esi.event.id as eventId, count(distinct esi.song.id) as total
            from EventSetlistItem esi
            where esi.event.id in :eventIds
              and esi.type = :type
              and esi.song is not null
            group by esi.event.id
            """)
    List<EventCountProjection> countDistinctSongsByEventIds(
            @Param("eventIds") List<UUID> eventIds,
            @Param("type") SetlistItemType type
    );
}
