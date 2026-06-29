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
}
