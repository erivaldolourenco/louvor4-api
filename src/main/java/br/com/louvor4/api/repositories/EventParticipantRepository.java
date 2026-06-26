package br.com.louvor4.api.repositories;

import br.com.louvor4.api.enums.EventParticipantStatus;
import br.com.louvor4.api.models.EventParticipant;
import br.com.louvor4.api.repositories.projections.EventCountProjection;
import br.com.louvor4.api.repositories.projections.EventProfileImageProjection;
import br.com.louvor4.api.repositories.projections.PastEventParticipantProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, UUID> {

    List<EventParticipant> findByMember_User_IdAndStatusAndEvent_StartAtGreaterThanEqualOrderByEvent_StartAtAsc(UUID userId, EventParticipantStatus status, LocalDateTime now);
    List<EventParticipant> findByEventId(UUID eventId);
    List<EventParticipant> findByEventIdIn(List<UUID> eventIds);
    List<EventParticipant> findByMember_Id(UUID memberId);
    List<EventParticipant> findByMember_IdAndEvent_StartAtGreaterThan(UUID memberId, java.time.LocalDateTime now);
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

    @Modifying
    @Transactional
    @Query("UPDATE EventParticipant ep SET ep.deletedAt = CURRENT_TIMESTAMP WHERE ep.event.id IN :eventIds AND ep.deletedAt IS NULL")
    void softDeleteByEventIds(@Param("eventIds") List<UUID> eventIds);

    // Consulta nativa para histórico: inclui eventos de projetos deletados (bypassa @SQLRestriction)
    @Query(
        value = """
            SELECT CAST(ep.id AS text)              AS participantId,
                   ep.status                        AS participantStatus,
                   CAST(e.id AS text)               AS eventId,
                   e.title                          AS eventTitle,
                   e.description                    AS eventDescription,
                   e.start_at                       AS eventStartAt,
                   e.location                       AS eventLocation,
                   CAST(mp.id AS text)              AS projectId,
                   mp.name                          AS projectName,
                   mp.profile_image                 AS projectProfileImage,
                   (SELECT count(DISTINCT ep2.project_member_id)
                    FROM event_participants ep2
                    WHERE ep2.event_id = e.id)::integer AS participantsCount,
                   (SELECT count(DISTINCT esi.song_id)
                    FROM event_setlist_items esi
                    WHERE esi.event_id = e.id AND esi.song_id IS NOT NULL)::integer AS repertoireCount
            FROM event_participants ep
            JOIN events e  ON e.id  = ep.event_id
            JOIN music_project_members m  ON m.id  = ep.project_member_id
            JOIN music_projects mp ON mp.id = m.music_project_id
            WHERE m.user_id  = CAST(:userId AS uuid)
              AND ep.status  = 'ACCEPTED'
              AND e.start_at < :now
            ORDER BY e.start_at DESC
            """,
        countQuery = """
            SELECT count(*) FROM event_participants ep
            JOIN events e ON e.id = ep.event_id
            JOIN music_project_members m ON m.id = ep.project_member_id
            WHERE m.user_id = CAST(:userId AS uuid)
              AND ep.status = 'ACCEPTED'
              AND e.start_at < :now
            """,
        nativeQuery = true
    )
    Page<PastEventParticipantProjection> findPastByUserIncludingDeletedProjects(
            @Param("userId") UUID userId,
            @Param("now") LocalDateTime now,
            Pageable pageable);

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
