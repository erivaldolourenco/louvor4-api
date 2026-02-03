package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
        from EventSong es
        where es.event.id = :eventId
    """)
    Integer countSongsByEventId(@Param("eventId") UUID eventId);


}
