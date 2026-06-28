package br.com.louvor4.entitlement.repositories;

import br.com.louvor4.entitlement.enums.SubscriptionStatus;
import br.com.louvor4.entitlement.models.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    @Query("""
            SELECT s FROM Subscription s
            JOIN FETCH s.plan
            WHERE s.userId = :userId
            AND s.status = :status
            """)
    Optional<Subscription> findActiveByUserId(
            @Param("userId") UUID userId,
            @Param("status") SubscriptionStatus status
    );
}
