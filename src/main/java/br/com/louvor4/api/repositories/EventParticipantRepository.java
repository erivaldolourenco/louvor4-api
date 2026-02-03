package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.EventParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, UUID> {

    List<EventParticipant> findByMember_User_IdAndEvent_StartAtGreaterThanEqualOrderByEvent_StartAtAsc(UUID userId, LocalDateTime now);
    List<EventParticipant> findByEventId(UUID eventId);
    List<EventParticipant> findByEventIdIn(List<UUID> eventIds);
    boolean existsByEventIdAndMemberIdAndSkillId(UUID eventId, UUID memberId, UUID skillId);
    void deleteByEventId(UUID eventId);
    Optional<EventParticipant> findByEventIdAndMemberUserId(UUID eventId, UUID userId);

}
