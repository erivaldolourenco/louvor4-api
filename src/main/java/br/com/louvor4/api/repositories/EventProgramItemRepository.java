package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.EventProgramItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EventProgramItemRepository extends JpaRepository<EventProgramItem, UUID> {

    List<EventProgramItem> findByEventIdOrderByPositionAsc(UUID eventId);

    @Query("""
            select coalesce(max(p.position), 0)
            from EventProgramItem p
            where p.event.id = :eventId
            """)
    Integer findMaxPositionByEventId(@Param("eventId") UUID eventId);

    void deleteBySetlistItemId(UUID setlistItemId);
}
