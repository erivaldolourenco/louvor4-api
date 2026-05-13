package br.com.louvor4.api.repositories;

import br.com.louvor4.api.enums.ReminderStatus;
import br.com.louvor4.api.models.EventReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface EventReminderRepository extends JpaRepository<EventReminder, UUID> {

    List<EventReminder> findByStatusAndScheduledForLessThanEqual(
            ReminderStatus status, LocalDateTime now);

    List<EventReminder> findByEventIdAndStatusIn(UUID eventId, List<ReminderStatus> statuses);

    List<EventReminder> findByStatusAndUpdatedAtBefore(
            ReminderStatus status, LocalDateTime threshold);

    @Modifying
    @Transactional
    @Query("""
        UPDATE EventReminder e
           SET e.status = :newStatus, e.updatedAt = :now
         WHERE e.id = :id AND e.status = :currentStatus
        """)
    int tryUpdateStatus(
            @Param("id") UUID id,
            @Param("currentStatus") ReminderStatus currentStatus,
            @Param("newStatus") ReminderStatus newStatus,
            @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("""
        UPDATE EventReminder e
           SET e.status = 'SENT', e.sentAt = :now, e.updatedAt = :now
         WHERE e.id = :id
        """)
    void markAsSent(@Param("id") UUID id, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("""
        UPDATE EventReminder e
           SET e.status = 'FAILED', e.errorMessage = :msg, e.updatedAt = :now
         WHERE e.id = :id
        """)
    void markAsFailed(@Param("id") UUID id, @Param("msg") String msg, @Param("now") LocalDateTime now);

    @Modifying
    @Transactional
    @Query("""
        UPDATE EventReminder e
           SET e.status = 'CANCELLED', e.updatedAt = :now
         WHERE e.id = :id AND e.status IN ('PENDING', 'PROCESSING')
        """)
    int cancelById(@Param("id") UUID id, @Param("now") LocalDateTime now);
}
