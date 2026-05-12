package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.EventScheduleItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EventScheduleItemRepository extends JpaRepository<EventScheduleItem, UUID> {

    List<EventScheduleItem> findByEventIdOrderByPositionAsc(UUID eventId);

    @Query("""
            select coalesce(max(s.position), 0)
            from EventScheduleItem s
            where s.event.id = :eventId
            """)
    Integer findMaxPositionByEventId(@Param("eventId") UUID eventId);

    void deleteBySetlistItemId(UUID setlistItemId);
}
