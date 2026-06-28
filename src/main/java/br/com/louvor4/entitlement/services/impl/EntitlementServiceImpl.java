package br.com.louvor4.entitlement.services.impl;

import br.com.louvor4.entitlement.enums.SubscriptionStatus;
import br.com.louvor4.entitlement.exceptions.PlanLimitExceededException;
import br.com.louvor4.entitlement.models.Subscription;
import br.com.louvor4.entitlement.models.UsageCounter;
import br.com.louvor4.entitlement.repositories.PlanEntitlementRepository;
import br.com.louvor4.entitlement.repositories.SubscriptionOverrideRepository;
import br.com.louvor4.entitlement.repositories.SubscriptionRepository;
import br.com.louvor4.entitlement.repositories.UsageCounterRepository;
import br.com.louvor4.entitlement.services.EntitlementService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EntitlementServiceImpl implements EntitlementService {

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionOverrideRepository overrideRepository;
    private final PlanEntitlementRepository planEntitlementRepository;
    private final UsageCounterRepository usageCounterRepository;

    private final ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

    public EntitlementServiceImpl(SubscriptionRepository subscriptionRepository,
                                  SubscriptionOverrideRepository overrideRepository,
                                  PlanEntitlementRepository planEntitlementRepository,
                                  UsageCounterRepository usageCounterRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.overrideRepository = overrideRepository;
        this.planEntitlementRepository = planEntitlementRepository;
        this.usageCounterRepository = usageCounterRepository;
    }

    @Override
    public String getPlanName(UUID userId) {
        return findActiveSubscription(userId).getPlan().getName();
    }

    @Override
    public boolean hasFeature(UUID userId, String key) {
        Subscription sub = findActiveSubscription(userId);
        String value = resolve(sub.getId(), sub.getPlan().getId(), key);
        return Boolean.parseBoolean(value);
    }

    @Override
    public int getLimit(UUID userId, String key) {
        Subscription sub = findActiveSubscription(userId);
        String value = resolve(sub.getId(), sub.getPlan().getId(), key);
        return Integer.parseInt(value);
    }

    @Override
    public void enforceLimit(UUID userId, String key, long current) {
        int limit = getLimit(userId, key);
        if (limit != -1 && current >= limit) {
            throw new PlanLimitExceededException(key, limit);
        }
    }

    @Override
    @Transactional
    public void consumeQuota(UUID userId, String key) {
        Subscription sub = findActiveSubscription(userId);
        String value = resolve(sub.getId(), sub.getPlan().getId(), key);
        int quota = Integer.parseInt(value);

        if (quota == -1) return;
        if (quota == 0) throw new PlanLimitExceededException(key, 0);

        LocalDate periodStart = LocalDate.now().withDayOfMonth(1);
        UsageCounter counter = usageCounterRepository
                .findBySubscriptionIdAndEntitlementKeyAndPeriodStart(sub.getId(), key, periodStart)
                .orElseGet(() -> {
                    UsageCounter c = new UsageCounter();
                    c.setSubscriptionId(sub.getId());
                    c.setEntitlementKey(key);
                    c.setCount(0);
                    c.setPeriodStart(periodStart);
                    return c;
                });

        if (counter.getCount() >= quota) {
            throw new PlanLimitExceededException(key, quota);
        }

        counter.setCount(counter.getCount() + 1);
        usageCounterRepository.save(counter);
    }

    @Override
    public void invalidateCache(UUID subscriptionId) {
        String prefix = subscriptionId.toString();
        cache.keySet().removeIf(k -> k.startsWith(prefix));
    }

    private String resolve(UUID subscriptionId, UUID planId, String key) {
        String cacheKey = subscriptionId + ":" + key;
        return cache.computeIfAbsent(cacheKey, k -> {
            return overrideRepository.findActiveOverride(subscriptionId, key)
                    .map(o -> o.getValue())
                    .orElseGet(() -> planEntitlementRepository
                            .findByPlanIdAndEntitlementKey(planId, key)
                            .map(pe -> pe.getValue())
                            .orElse("false"));
        });
    }

    private Subscription findActiveSubscription(UUID userId) {
        return subscriptionRepository.findActiveByUserId(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new IllegalStateException(
                        "Nenhuma assinatura ativa encontrada para o usuário: " + userId));
    }
}
