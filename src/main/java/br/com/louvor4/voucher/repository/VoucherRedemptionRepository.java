package br.com.louvor4.voucher.repository;

import br.com.louvor4.voucher.model.VoucherRedemption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface VoucherRedemptionRepository extends JpaRepository<VoucherRedemption, UUID> {

    boolean existsByVoucherIdAndUserId(UUID voucherId, UUID userId);

    long countByVoucherId(UUID voucherId);

    @Query("""
            SELECT r FROM VoucherRedemption r
            WHERE r.validUntil < :now
            AND r.reverted = false
            """)
    List<VoucherRedemption> findExpiredNotReverted(@Param("now") LocalDateTime now);

    /**
     * Resgates ainda não encerrados da assinatura, do mais antigo para o mais novo.
     * Inclui os já vencidos que o scheduler ainda não processou.
     */
    @Query("""
            SELECT r FROM VoucherRedemption r
            WHERE r.subscriptionId = :subscriptionId
            AND r.reverted = false
            ORDER BY r.redeemedAt ASC
            """)
    List<VoucherRedemption> findNotRevertedBySubscriptionId(@Param("subscriptionId") UUID subscriptionId);

    /** Existe outro resgate em vigor na assinatura, além do informado? */
    @Query("""
            SELECT COUNT(r) > 0 FROM VoucherRedemption r
            WHERE r.subscriptionId = :subscriptionId
            AND r.id <> :excludeId
            AND r.reverted = false
            AND r.validUntil > :now
            """)
    boolean existsOtherActive(@Param("subscriptionId") UUID subscriptionId,
                              @Param("excludeId") UUID excludeId,
                              @Param("now") LocalDateTime now);
}
