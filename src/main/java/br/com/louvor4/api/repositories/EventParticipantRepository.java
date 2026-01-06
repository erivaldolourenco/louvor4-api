package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.EventParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, UUID> {
    boolean existsByEvent_IdAndUser_Id(UUID eventId, UUID userId);
    List<EventParticipant> getEventParticipantByUser_Id(UUID userId);
    List<EventParticipant> findByUser_IdOrderByEvent_StartAtAsc(UUID userId);
}
