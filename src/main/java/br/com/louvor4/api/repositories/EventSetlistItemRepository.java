package br.com.louvor4.api.repositories;

import br.com.louvor4.api.enums.SetlistItemType;
import br.com.louvor4.api.models.EventSetlistItem;
import br.com.louvor4.api.repositories.projections.EventCountProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

public interface EventSetlistItemRepository extends JpaRepository<EventSetlistItem, UUID> {

    List<EventSetlistItem> findByEventIdOrderBySequenceAsc(UUID eventId);

    List<EventSetlistItem> findByEventIdAndTypeOrderBySequenceAsc(UUID eventId, SetlistItemType type);

    List<EventSetlistItem> findByEventIdInAndType(List<UUID> eventIds, SetlistItemType type);

    void deleteByAddedBy_IdIn(List<UUID> participantIds);

    // @Modifying para executar como DELETE imediato: a versão derivada (deleteBy...) apenas
    // marca as entidades para remoção no persistence context, sem flush. Como os call sites
    // seguem com um deleteAllInBatch imediato em event_participants, a ordem física no banco
    // ficava invertida e violava a FK de event_setlist_items.added_by_participant_id.
    @Modifying
    @Transactional
    @Query("delete from EventSetlistItem esi where esi.addedBy.id in :participantIds and esi.event.startAt > :now")
    void deleteByAddedBy_IdInAndEvent_StartAtGreaterThan(
            @Param("participantIds") List<UUID> participantIds,
            @Param("now") java.time.LocalDateTime now
    );

    @Query("select esi.id from EventSetlistItem esi where esi.addedBy.id in :participantIds and esi.event.startAt > :now")
    List<UUID> findIdsByAddedBy_IdInAndEvent_StartAtGreaterThan(
            @Param("participantIds") List<UUID> participantIds,
            @Param("now") java.time.LocalDateTime now
    );

    void deleteByEventIdIn(List<UUID> eventIds);

    boolean existsBySong_Id(UUID songId);

    @Query("""
            select distinct esi.event.musicProject.id
            from EventSetlistItem esi
            where esi.song.id = :songId
            """)
    List<UUID> findProjectIdsBySongId(@Param("songId") UUID songId);

    @Query("""
            select distinct esi.event.id
            from EventSetlistItem esi
            where esi.song.id = :songId
            """)
    List<UUID> findEventIdsBySongId(@Param("songId") UUID songId);

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
