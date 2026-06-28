package br.com.louvor4.entitlement.repositories;

import br.com.louvor4.entitlement.models.UsageCounter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface UsageCounterRepository extends JpaRepository<UsageCounter, UUID> {

    Optional<UsageCounter> findBySubscriptionIdAndEntitlementKeyAndPeriodStart(
            UUID subscriptionId,
            String entitlementKey,
            LocalDate periodStart
    );
}
