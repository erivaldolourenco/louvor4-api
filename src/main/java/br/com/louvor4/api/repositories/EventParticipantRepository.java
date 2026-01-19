package br.com.louvor4.api.repositories;

import br.com.louvor4.api.models.EventParticipant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, UUID> {

    List<EventParticipant> findByMember_User_IdOrderByEvent_StartAtAsc(UUID userId);
    List<EventParticipant> findByEventId(UUID eventId);
    boolean existsByEventIdAndMemberIdAndSkillId(UUID eventId, UUID memberId, UUID skillId);
    void deleteByEventId(UUID eventId);
    Optional<EventParticipant> findByEventIdAndMemberUserId(UUID eventId, UUID userId);
}
