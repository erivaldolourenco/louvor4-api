package br.com.louvor4.voucher.service.impl;

import br.com.louvor4.api.config.security.CurrentUserProvider;
import br.com.louvor4.api.models.User;
import br.com.louvor4.entitlement.enums.SubscriptionStatus;
import br.com.louvor4.entitlement.models.Plans;
import br.com.louvor4.entitlement.models.Subscription;
import br.com.louvor4.entitlement.repositories.PlansRepository;
import br.com.louvor4.entitlement.repositories.SubscriptionRepository;
import br.com.louvor4.entitlement.services.EntitlementService;
import br.com.louvor4.voucher.exception.VoucherException;
import br.com.louvor4.voucher.model.Voucher;
import br.com.louvor4.voucher.model.VoucherRedemption;
import br.com.louvor4.voucher.repository.VoucherRedemptionRepository;
import br.com.louvor4.voucher.repository.VoucherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VoucherServiceImplTest {

    @Mock VoucherRepository voucherRepository;
    @Mock VoucherRedemptionRepository redemptionRepository;
    @Mock PlansRepository plansRepository;
    @Mock SubscriptionRepository subscriptionRepository;
    @Mock EntitlementService entitlementService;
    @Mock CurrentUserProvider currentUserProvider;

    @InjectMocks VoucherServiceImpl service;

    private Plans free;
    private Plans partner;
    private Plans pro;
    private User user;
    private Subscription subscription;

    @BeforeEach
    void setUp() {
        free = plan("FREE");
        partner = plan("PARTNER");
        pro = plan("PRO");

        user = new User();
        user.setId(UUID.randomUUID());

        subscription = new Subscription();
        subscription.setId(UUID.randomUUID());
        subscription.setPlan(free);
    }

    @Nested
    class Redeem {

        private Voucher voucher;

        @BeforeEach
        void setUp() {
            voucher = voucher("IEQCP2026", partner);
            when(currentUserProvider.get()).thenReturn(user);
            when(voucherRepository.findActiveByCode("IEQCP2026")).thenReturn(Optional.of(voucher));
            when(subscriptionRepository.findActiveByUserId(user.getId(), SubscriptionStatus.ACTIVE))
                    .thenReturn(Optional.of(subscription));
        }

        @Test
        void semVoucherEmAndamento_guardaPlanoAtualComoAnterior() {
            when(redemptionRepository.findNotRevertedBySubscriptionId(subscription.getId())).thenReturn(List.of());

            service.redeem("ieqcp2026 ");

            VoucherRedemption saved = captureSavedRedemption();
            assertThat(saved.getPreviousPlanId()).isEqualTo(free.getId());
            assertThat(subscription.getPlan()).isEqualTo(partner);
            verify(entitlementService).invalidateCache(subscription.getId());
        }

        /**
         * Caso real: resgatou PARCEIRO191308 (Gratuito → Parceiro) e, ainda com ele em vigor,
         * resgatou IEQCP2026. O novo precisa herdar "Gratuito" como plano anterior e encerrar o antigo;
         * antes, guardava "Parceiro" e o antigo, ao vencer, devolvia o Gratuito por cima do novo.
         */
        @Test
        void comVoucherEmAndamento_herdaPlanoOriginalEEncerraOAnterior() {
            subscription.setPlan(partner); // veio do primeiro voucher
            VoucherRedemption first = redemption(free, LocalDateTime.now().plusDays(6));
            when(redemptionRepository.findNotRevertedBySubscriptionId(subscription.getId())).thenReturn(List.of(first));

            service.redeem("IEQCP2026");

            VoucherRedemption saved = captureSavedRedemption();
            assertThat(saved.getPreviousPlanId()).isEqualTo(free.getId());
            assertThat(first.isReverted()).isTrue();
            verify(redemptionRepository).saveAll(List.of(first));
            assertThat(subscription.getPlan()).isEqualTo(partner);
        }

        @Test
        void comVariosEmAndamento_usaOPlanoDoMaisAntigo() {
            subscription.setPlan(partner);
            VoucherRedemption oldest = redemption(free, LocalDateTime.now().minusMinutes(5)); // vencido, ainda não revertido
            VoucherRedemption newer = redemption(partner, LocalDateTime.now().plusDays(30));
            when(redemptionRepository.findNotRevertedBySubscriptionId(subscription.getId()))
                    .thenReturn(List.of(oldest, newer));

            service.redeem("IEQCP2026");

            assertThat(captureSavedRedemption().getPreviousPlanId()).isEqualTo(free.getId());
            assertThat(oldest.isReverted()).isTrue();
            assertThat(newer.isReverted()).isTrue();
        }

        @Test
        void resgateDuplicadoSimultaneo_viraMensagemDeVoucherJaUtilizado() {
            when(redemptionRepository.findNotRevertedBySubscriptionId(subscription.getId())).thenReturn(List.of());
            when(redemptionRepository.saveAndFlush(any()))
                    .thenThrow(new DataIntegrityViolationException(VoucherRedemption.UQ_VOUCHER_USER));

            assertThatThrownBy(() -> service.redeem("IEQCP2026"))
                    .isInstanceOf(VoucherException.class)
                    .hasMessage("Você já utilizou este voucher.");
            assertThat(subscription.getPlan()).isEqualTo(free);
            verify(subscriptionRepository, never()).save(any());
        }

        private VoucherRedemption captureSavedRedemption() {
            var captor = ArgumentCaptor.forClass(VoucherRedemption.class);
            verify(redemptionRepository).saveAndFlush(captor.capture());
            return captor.getValue();
        }
    }

    @Nested
    class RevertExpired {

        private Voucher partnerVoucher;
        private VoucherRedemption expired;

        @BeforeEach
        void setUp() {
            partnerVoucher = voucher("PARCEIRO191308", partner);
            expired = redemption(free, LocalDateTime.now().minusHours(1));
            expired.setVoucherId(partnerVoucher.getId());
            when(redemptionRepository.findExpiredNotReverted(any())).thenReturn(List.of(expired));
        }

        @Test
        void semOutroVoucher_devolvePlanoAnterior() {
            subscription.setPlan(partner);
            when(redemptionRepository.existsOtherActive(eq(subscription.getId()), eq(expired.getId()), any()))
                    .thenReturn(false);
            when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
            when(voucherRepository.findById(partnerVoucher.getId())).thenReturn(Optional.of(partnerVoucher));
            when(plansRepository.findById(free.getId())).thenReturn(Optional.of(free));

            service.revertExpired();

            assertThat(subscription.getPlan()).isEqualTo(free);
            assertThat(expired.isReverted()).isTrue();
            verify(entitlementService).invalidateCache(subscription.getId());
        }

        /** Dado já gravado antes da correção: dois resgates em andamento na mesma assinatura. */
        @Test
        void comOutroVoucherEmVigor_soEncerraSemMexerNoPlano() {
            subscription.setPlan(partner);
            when(redemptionRepository.existsOtherActive(eq(subscription.getId()), eq(expired.getId()), any()))
                    .thenReturn(true);

            service.revertExpired();

            assertThat(subscription.getPlan()).isEqualTo(partner);
            assertThat(expired.isReverted()).isTrue();
            verify(redemptionRepository).save(expired);
            verifyNoInteractions(subscriptionRepository, entitlementService);
        }

        @Test
        void usuarioMudouDePlano_naoRebaixa() {
            subscription.setPlan(pro); // assinou o PRO enquanto o voucher Parceiro valia
            when(redemptionRepository.existsOtherActive(eq(subscription.getId()), eq(expired.getId()), any()))
                    .thenReturn(false);
            when(subscriptionRepository.findById(subscription.getId())).thenReturn(Optional.of(subscription));
            when(voucherRepository.findById(partnerVoucher.getId())).thenReturn(Optional.of(partnerVoucher));

            service.revertExpired();

            assertThat(subscription.getPlan()).isEqualTo(pro);
            assertThat(expired.isReverted()).isTrue();
            verify(subscriptionRepository, never()).save(any());
            verifyNoInteractions(entitlementService);
        }
    }

    // ---------- helpers ----------

    private static Plans plan(String name) {
        var plan = new Plans();
        plan.setId(UUID.randomUUID());
        plan.setName(name);
        return plan;
    }

    private static Voucher voucher(String code, Plans plan) {
        var voucher = new Voucher();
        voucher.setId(UUID.randomUUID());
        voucher.setCode(code);
        voucher.setPlan(plan);
        voucher.setDurationDays(365);
        return voucher;
    }

    private VoucherRedemption redemption(Plans previousPlan, LocalDateTime validUntil) {
        var redemption = new VoucherRedemption();
        redemption.setId(UUID.randomUUID());
        redemption.setVoucherId(UUID.randomUUID());
        redemption.setUserId(user.getId());
        redemption.setSubscriptionId(subscription.getId());
        redemption.setPreviousPlanId(previousPlan.getId());
        redemption.setValidUntil(validUntil);
        return redemption;
    }
}
