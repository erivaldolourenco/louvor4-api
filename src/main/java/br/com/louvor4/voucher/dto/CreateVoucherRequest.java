package br.com.louvor4.voucher.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record CreateVoucherRequest(
        @NotBlank String code,
        @NotNull UUID planId,
        @NotNull @Min(1) int durationDays,
        Integer maxUses,
        LocalDateTime expiresAt
) {}
