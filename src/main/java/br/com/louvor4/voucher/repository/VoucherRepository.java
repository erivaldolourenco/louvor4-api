package br.com.louvor4.voucher.repository;

import br.com.louvor4.voucher.model.Voucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VoucherRepository extends JpaRepository<Voucher, UUID> {

    @Query("""
            SELECT v FROM Voucher v
            JOIN FETCH v.plan
            WHERE v.code = :code
            AND v.active = true
            """)
    Optional<Voucher> findActiveByCode(@Param("code") String code);
}
