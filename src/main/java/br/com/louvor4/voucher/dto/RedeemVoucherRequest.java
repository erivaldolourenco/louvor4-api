package br.com.louvor4.voucher.dto;

import jakarta.validation.constraints.NotBlank;

public record RedeemVoucherRequest(
        @NotBlank String code
) {}
