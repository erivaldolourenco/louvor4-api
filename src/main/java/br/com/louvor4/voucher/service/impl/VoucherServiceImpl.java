package br.com.louvor4.voucher.service.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.models.User;
import br.com.louvor4.entitlement.enums.SubscriptionStatus;
import br.com.louvor4.entitlement.models.Plans;
import br.com.louvor4.entitlement.models.Subscription;
import br.com.louvor4.entitlement.repositories.PlansRepository;
import br.com.louvor4.entitlement.repositories.SubscriptionRepository;
import br.com.louvor4.entitlement.services.EntitlementService;
import br.com.louvor4.voucher.dto.CreateVoucherRequest;
import br.com.louvor4.voucher.dto.RedeemVoucherResponse;
import br.com.louvor4.voucher.dto.VoucherResponse;
import br.com.louvor4.voucher.exception.VoucherException;
import br.com.louvor4.voucher.model.Voucher;
import br.com.louvor4.voucher.model.VoucherRedemption;
import br.com.louvor4.voucher.repository.VoucherRedemptionRepository;
import br.com.louvor4.voucher.repository.VoucherRepository;
import br.com.louvor4.voucher.service.VoucherService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final VoucherRedemptionRepository redemptionRepository;
    private final PlansRepository plansRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final EntitlementService entitlementService;
    private final CurrentUserProvider currentUserProvider;

    public VoucherServiceImpl(VoucherRepository voucherRepository,
                              VoucherRedemptionRepository redemptionRepository,
                              PlansRepository plansRepository,
                              SubscriptionRepository subscriptionRepository,
                              EntitlementService entitlementService,
                              CurrentUserProvider currentUserProvider) {
        this.voucherRepository = voucherRepository;
        this.redemptionRepository = redemptionRepository;
        this.plansRepository = plansRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.entitlementService = entitlementService;
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    @Transactional
    public VoucherResponse create(CreateVoucherRequest request) {
        Plans plan = plansRepository.findById(request.planId())
                .orElseThrow(() -> new VoucherException("Plano não encontrado."));

        Voucher voucher = new Voucher();
        voucher.setCode(request.code().toUpperCase().trim());
        voucher.setPlan(plan);
        voucher.setDurationDays(request.durationDays());
        voucher.setMaxUses(request.maxUses());
        voucher.setExpiresAt(request.expiresAt());

        return toResponse(voucherRepository.save(voucher));
    }

    @Override
    @Transactional
    public RedeemVoucherResponse redeem(String code) {
        User user = currentUserProvider.get();

        Voucher voucher = voucherRepository.findActiveByCode(code.toUpperCase().trim())
                .orElseThrow(() -> new VoucherException("Voucher inválido ou inativo."));

        if (voucher.getExpiresAt() != null && voucher.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new VoucherException("Este voucher expirou.");
        }

        if (voucher.getMaxUses() != null) {
            long used = redemptionRepository.countByVoucherId(voucher.getId());
            if (used >= voucher.getMaxUses()) {
                throw new VoucherException("Este voucher já atingiu o limite de usos.");
            }
        }

        if (redemptionRepository.existsByVoucherIdAndUserId(voucher.getId(), user.getId())) {
            throw new VoucherException("Você já utilizou este voucher.");
        }

        Subscription subscription = subscriptionRepository
                .findActiveByUserId(user.getId(), SubscriptionStatus.ACTIVE)
                .orElseThrow(() -> new VoucherException("Nenhuma assinatura ativa encontrada."));

        // Se já há voucher em andamento, o plano atual veio dele. O plano "de verdade" do usuário
        // é o que estava antes do PRIMEIRO voucher: o novo herda esse valor e substitui os anteriores.
        // Sem isso, ao vencer, o voucher antigo devolvia o plano por cima do novo.
        UUID previousPlanId = subscription.getPlan().getId();
        List<VoucherRedemption> ongoing = redemptionRepository.findNotRevertedBySubscriptionId(subscription.getId());
        if (!ongoing.isEmpty()) {
            previousPlanId = ongoing.get(0).getPreviousPlanId();
            ongoing.forEach(current -> current.setReverted(true)); // encerrado: substituído pelo novo voucher
            redemptionRepository.saveAll(ongoing);
        }

        VoucherRedemption redemption = new VoucherRedemption();
        redemption.setVoucherId(voucher.getId());
        redemption.setUserId(user.getId());
        redemption.setSubscriptionId(subscription.getId());
        redemption.setPreviousPlanId(previousPlanId);
        redemption.setValidUntil(LocalDateTime.now().plusDays(voucher.getDurationDays()));

        try {
            // flush agora para a constraint única pegar dois resgates simultâneos do mesmo voucher
            redemptionRepository.saveAndFlush(redemption);
        } catch (DataIntegrityViolationException e) {
            throw new VoucherException("Você já utilizou este voucher.");
        }

        subscription.setPlan(voucher.getPlan());
        subscriptionRepository.save(subscription);
        entitlementService.invalidateCache(subscription.getId());

        return new RedeemVoucherResponse(voucher.getPlan().getName());
    }

    @Override
    @Transactional
    public void revertExpired() {
        LocalDateTime now = LocalDateTime.now();
        List<VoucherRedemption> expired = redemptionRepository.findExpiredNotReverted(now);

        for (VoucherRedemption redemption : expired) {
            redemption.setReverted(true);
            redemptionRepository.save(redemption);

            // outro voucher em vigor manda no plano da assinatura: não mexe
            if (redemptionRepository.existsOtherActive(redemption.getSubscriptionId(), redemption.getId(), now)) {
                continue;
            }

            subscriptionRepository.findById(redemption.getSubscriptionId()).ifPresent(sub -> {
                // só devolve se a assinatura ainda está no plano que o voucher deu;
                // se o usuário trocou de plano no meio (ex.: assinou o PRO), não rebaixa
                if (!isOnVoucherPlan(sub, redemption)) {
                    return;
                }
                plansRepository.findById(redemption.getPreviousPlanId()).ifPresent(plan -> {
                    sub.setPlan(plan);
                    subscriptionRepository.save(sub);
                    entitlementService.invalidateCache(sub.getId());
                });
            });
        }
    }

    private boolean isOnVoucherPlan(Subscription subscription, VoucherRedemption redemption) {
        return voucherRepository.findById(redemption.getVoucherId())
                .map(voucher -> voucher.getPlan().getId().equals(subscription.getPlan().getId()))
                .orElse(false);
    }

    private VoucherResponse toResponse(Voucher v) {
        return new VoucherResponse(
                v.getId(),
                v.getCode(),
                v.getPlan().getName(),
                v.getDurationDays(),
                v.getMaxUses(),
                v.getExpiresAt(),
                v.isActive()
        );
    }
}
