package br.com.louvor4.api.repositories;

import br.com.louvor4.api.enums.SetlistItemType;
import br.com.louvor4.api.models.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {
    List<Event> findAllByMusicProject_IdAndStartAtGreaterThanEqualOrderByStartAtAsc(UUID projectId, LocalDateTime now);

    List<Event> findAllByMusicProjectIdAndStartAtBetweenOrderByStartAtAsc(UUID projectId, LocalDateTime start, LocalDateTime end);

    @Query("""
        select count(distinct ep.member.id)
        from EventParticipant ep
        where ep.event.id = :eventId
    """)
    Integer countParticipantsByEventId(@Param("eventId") UUID eventId);

    @Query("""
        select count(distinct es.song.id)
        from EventSetlistItem es
        where es.event.id = :eventId
          and es.type = :type
          and es.song is not null
    """)
    Integer countSongsByEventId(@Param("eventId") UUID eventId, @Param("type") SetlistItemType type);

    @Query("SELECT e.musicProject.id FROM Event e WHERE e.id = :eventId")
    UUID findProjectIdByEventId(@Param("eventId") UUID eventId);

    @Query("SELECT e.id FROM Event e WHERE e.musicProject.id = :projectId")
    List<UUID> findIdsByMusicProjectId(@Param("projectId") UUID projectId);

    @Modifying
    @Transactional
    @Query("UPDATE Event e SET e.deletedAt = CURRENT_TIMESTAMP WHERE e.musicProject.id = :projectId AND e.deletedAt IS NULL")
    void softDeleteByProjectId(@Param("projectId") UUID projectId);

    void deleteByMusicProject_Id(UUID projectId);

}
