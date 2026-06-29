package br.com.louvor4.voucher.scheduler;

import br.com.louvor4.voucher.service.VoucherService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class VoucherExpirationScheduler {

    private final VoucherService voucherService;

    public VoucherExpirationScheduler(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.HOURS)
    public void revertExpiredVouchers() {
        voucherService.revertExpired();
    }
}
