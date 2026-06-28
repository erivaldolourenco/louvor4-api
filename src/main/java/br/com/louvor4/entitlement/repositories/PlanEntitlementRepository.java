package br.com.louvor4.entitlement.repositories;

import br.com.louvor4.entitlement.models.PlanEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PlanEntitlementRepository extends JpaRepository<PlanEntitlement, UUID> {

    List<PlanEntitlement> findByPlan_Id(UUID planId);

    @Query("""
            SELECT pe FROM PlanEntitlement pe
            JOIN FETCH pe.entitlement e
            WHERE pe.plan.id = :planId
            AND e.key = :entitlementKey
            """)
    Optional<PlanEntitlement> findByPlanIdAndEntitlementKey(
            @Param("planId") UUID planId,
            @Param("entitlementKey") String entitlementKey
    );
}
