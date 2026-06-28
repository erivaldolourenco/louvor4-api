package br.com.louvor4.entitlement.repositories;

import br.com.louvor4.entitlement.models.SubscriptionOverride;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SubscriptionOverrideRepository extends JpaRepository<SubscriptionOverride, UUID> {

    @Query("""
            SELECT so FROM SubscriptionOverride so
            WHERE so.subscriptionId = :subscriptionId
            AND so.entitlementKey = :key
            AND so.active = true
            AND (so.expiresAt IS NULL OR so.expiresAt > CURRENT_TIMESTAMP)
            """)
    Optional<SubscriptionOverride> findActiveOverride(
            @Param("subscriptionId") UUID subscriptionId,
            @Param("key") String key
    );
}
