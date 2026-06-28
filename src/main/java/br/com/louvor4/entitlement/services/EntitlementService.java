package br.com.louvor4.entitlement.services;

import java.util.UUID;

public interface EntitlementService {

    String getPlanName(UUID userId);

    boolean hasFeature(UUID userId, String key);

    int getLimit(UUID userId, String key);

    void enforceLimit(UUID userId, String key, long current);

    void consumeQuota(UUID userId, String key);

    void invalidateCache(UUID subscriptionId);
}
