package br.com.louvor4.entitlement.aspect;

import br.com.louvor4.entitlement.exceptions.PlanLimitExceededException;
import br.com.louvor4.entitlement.services.EntitlementService;
import br.com.louvor4.entitlement.services.UserIdProvider;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Aspect
@Component
public class EntitlementAspect {

    private final EntitlementService entitlementService;
    private final UserIdProvider userIdProvider;

    public EntitlementAspect(EntitlementService entitlementService, UserIdProvider userIdProvider) {
        this.entitlementService = entitlementService;
        this.userIdProvider = userIdProvider;
    }

    @Around("@annotation(requiresPlan)")
    public Object enforce(ProceedingJoinPoint pjp, RequiresPlan requiresPlan) throws Throwable {
        UUID userId = userIdProvider.getCurrentUserId();

        if (!requiresPlan.feature().isEmpty()) {
            if (!entitlementService.hasFeature(userId, requiresPlan.feature())) {
                throw new PlanLimitExceededException(requiresPlan.feature(), 0);
            }
        }

        if (!requiresPlan.quota().isEmpty()) {
            entitlementService.consumeQuota(userId, requiresPlan.quota());
        }

        return pjp.proceed();
    }
}
