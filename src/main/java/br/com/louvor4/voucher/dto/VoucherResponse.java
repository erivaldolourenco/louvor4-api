package br.com.louvor4.voucher.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record VoucherResponse(
        UUID id,
        String code,
        String planName,
        int durationDays,
        Integer maxUses,
        LocalDateTime expiresAt,
        boolean active
) {}
