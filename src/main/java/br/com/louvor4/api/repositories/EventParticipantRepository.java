package br.com.louvor4.api.repositories;

import br.com.louvor4.api.enums.EventParticipantStatus;
import br.com.louvor4.api.models.EventParticipant;
import br.com.louvor4.api.repositories.projections.EventCountProjection;
import br.com.louvor4.api.repositories.projections.EventProfileImageProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, UUID> {

    List<EventParticipant> findByMember_User_IdAndStatusAndEvent_StartAtGreaterThanEqualOrderByEvent_StartAtAsc(UUID userId, EventParticipantStatus status, LocalDateTime now);
    List<EventParticipant> findByEventId(UUID eventId);
    List<EventParticipant> findByEventIdIn(List<UUID> eventIds);
    List<EventParticipant> findByMember_Id(UUID memberId);
    boolean existsByEventIdAndMemberIdAndSkillId(UUID eventId, UUID memberId, UUID skillId);
    void deleteByEventId(UUID eventId);
    Optional<EventParticipant> findByEventIdAndMemberUserId(UUID eventId, UUID userId);
    Optional<EventParticipant> findByIdAndMemberUserId(UUID id, UUID userId);

    @Query("""
            select distinct ep
            from EventParticipant ep
            join fetch ep.event e
            join fetch e.musicProject
            join fetch ep.member m
            join fetch m.user u
            where u.id = :userId
              and ep.status = :status
              and e.startAt >= :now
            order by e.startAt asc
            """)
    List<EventParticipant> findAcceptedByUserWithEventAndProjectAndMemberUser(
            @Param("userId") UUID userId,
            @Param("status") EventParticipantStatus status,
            @Param("now") LocalDateTime now
    );

    @Query("""
            select ep.event.id as eventId, count(distinct ep.member.id) as total
            from EventParticipant ep
            where ep.event.id in :eventIds
            group by ep.event.id
            """)
    List<EventCountProjection> countDistinctMembersByEventIds(@Param("eventIds") List<UUID> eventIds);

    @Query("""
            select ep.event.id as eventId, coalesce(u.profileImage, '') as profileImage
            from EventParticipant ep
            join ep.member m
            join m.user u
            where ep.event.id in :eventIds
            """)
    List<EventProfileImageProjection> findProfileImagesByEventIds(@Param("eventIds") List<UUID> eventIds);

}
