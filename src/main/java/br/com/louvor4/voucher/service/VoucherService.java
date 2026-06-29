package br.com.louvor4.voucher.service;

import br.com.louvor4.voucher.dto.CreateVoucherRequest;
import br.com.louvor4.voucher.dto.VoucherResponse;

public interface VoucherService {
    VoucherResponse create(CreateVoucherRequest request);
    void redeem(String code);
    void revertExpired();
}
