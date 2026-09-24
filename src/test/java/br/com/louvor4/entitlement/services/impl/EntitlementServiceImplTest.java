package br.com.louvor4.entitlement.services.impl;

import br.com.louvor4.entitlement.enums.SubscriptionStatus;
import br.com.louvor4.entitlement.exceptions.EntitlementMisconfiguredException;
import br.com.louvor4.entitlement.exceptions.PlanLimitExceededException;
import br.com.louvor4.entitlement.models.PlanEntitlement;
import br.com.louvor4.entitlement.models.Plans;
import br.com.louvor4.entitlement.models.Subscription;
import br.com.louvor4.entitlement.repositories.PlanEntitlementRepository;
import br.com.louvor4.entitlement.repositories.SubscriptionOverrideRepository;
import br.com.louvor4.entitlement.repositories.SubscriptionRepository;
import br.com.louvor4.entitlement.repositories.UsageCounterRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntitlementServiceImplTest {

    @Mock SubscriptionRepository subscriptionRepository;
    @Mock SubscriptionOverrideRepository overrideRepository;
    @Mock PlanEntitlementRepository planEntitlementRepository;
    @Mock UsageCounterRepository usageCounterRepository;
    @InjectMocks EntitlementServiceImpl service;

    private final UUID userId = UUID.randomUUID();
    private Subscription subscription;
    private Plans plan;

    @BeforeEach
    void setUp() {
        plan = new Plans();
        plan.setId(UUID.randomUUID());
        plan.setName("FREE");
        subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setPlan(plan);
        when(subscriptionRepository.findActiveByUserId(userId, SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(subscription));
        when(overrideRepository.findActiveOverride(any(), any())).thenReturn(Optional.empty());
    }

    private void planValue(String key, String value) {
        PlanEntitlement pe = new PlanEntitlement();
        pe.setValue(value);
        when(planEntitlementRepository.findByPlanIdAndEntitlementKey(plan.getId(), key))
                .thenReturn(Optional.of(pe));
    }

    @Test
    void enforceLimit_throwsMisconfiguredWhenPlanHasNoValue() {
        when(planEntitlementRepository.findByPlanIdAndEntitlementKey(plan.getId(), "max_songs"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enforceLimit(userId, "max_songs", 0))
                .isInstanceOf(EntitlementMisconfiguredException.class)
                .hasMessageContaining("max_songs")
                .hasMessageContaining("FREE");
    }

    @Test
    void getLimit_throwsMisconfiguredWhenValueIsNotNumeric() {
        planValue("max_songs", "false");

        assertThatThrownBy(() -> service.getLimit(userId, "max_songs"))
                .isInstanceOf(EntitlementMisconfiguredException.class)
                .hasMessageContaining("'false'");
    }

    @Test
    void getLimit_doesNotCacheMissingValue() {
        when(planEntitlementRepository.findByPlanIdAndEntitlementKey(plan.getId(), "max_songs"))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getLimit(userId, "max_songs"))
                .isInstanceOf(EntitlementMisconfiguredException.class);

        planValue("max_songs", "7");

        assertThat(service.getLimit(userId, "max_songs")).isEqualTo(7);
        verify(planEntitlementRepository, times(2)).findByPlanIdAndEntitlementKey(eq(plan.getId()), eq("max_songs"));
    }

    @Test
    void enforceLimit_throwsPlanLimitExceededWhenAtLimit() {
        planValue("max_songs", "7");

        assertThatThrownBy(() -> service.enforceLimit(userId, "max_songs", 7))
                .isInstanceOf(PlanLimitExceededException.class);
    }

    @Test
    void hasFeature_defaultsToFalseWhenPlanHasNoValue() {
        when(planEntitlementRepository.findByPlanIdAndEntitlementKey(plan.getId(), "upload_audio"))
                .thenReturn(Optional.empty());

        assertThat(service.hasFeature(userId, "upload_audio")).isFalse();
    }
}
