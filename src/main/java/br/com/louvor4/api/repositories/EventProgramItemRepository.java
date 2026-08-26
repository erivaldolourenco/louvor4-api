package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.EventProgramItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

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
    void deleteByEventIdIn(List<UUID> eventIds);

    // @Modifying para DELETE imediato: precisa rodar antes do delete em event_setlist_items
    // (FK event_program_items.setlist_item_id), sem depender de flush do persistence context.
    @Modifying
    @Transactional
    @Query("delete from EventProgramItem p where p.setlistItem.id in :setlistItemIds")
    void deleteBySetlistItemIdIn(@Param("setlistItemIds") List<UUID> setlistItemIds);
}
