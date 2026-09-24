package br.com.louvor4.entitlement.services.impl;

import br.com.louvor4.entitlement.enums.SubscriptionStatus;
import br.com.louvor4.entitlement.exceptions.EntitlementMisconfiguredException;
import br.com.louvor4.entitlement.exceptions.NoActiveSubscriptionException;
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

    private static final String DEFAULT_FLAG_VALUE = "false";

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
        return Boolean.parseBoolean(value != null ? value : DEFAULT_FLAG_VALUE);
    }

    @Override
    public int getLimit(UUID userId, String key) {
        Subscription sub = findActiveSubscription(userId);
        return resolveNumber(sub, key);
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
        int quota = resolveNumber(sub, key);

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

    /**
     * Override do cliente → valor do plano. Retorna null quando nenhum dos dois existe;
     * nesse caso nada é cacheado, para que corrigir o banco tenha efeito sem reiniciar a API.
     */
    private String resolve(UUID subscriptionId, UUID planId, String key) {
        String cacheKey = subscriptionId + ":" + key;
        return cache.computeIfAbsent(cacheKey, k -> overrideRepository.findActiveOverride(subscriptionId, key)
                .map(o -> o.getValue())
                .orElseGet(() -> planEntitlementRepository
                        .findByPlanIdAndEntitlementKey(planId, key)
                        .map(pe -> pe.getValue())
                        .orElse(null)));
    }

    private int resolveNumber(Subscription sub, String key) {
        String value = resolve(sub.getId(), sub.getPlan().getId(), key);
        if (value == null) {
            throw new EntitlementMisconfiguredException(sub.getPlan().getName(), key, null);
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            throw new EntitlementMisconfiguredException(sub.getPlan().getName(), key, value);
        }
    }

    private Subscription findActiveSubscription(UUID userId) {
        return subscriptionRepository.findActiveByUserId(userId, SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new NoActiveSubscriptionException(userId));
    }
}
