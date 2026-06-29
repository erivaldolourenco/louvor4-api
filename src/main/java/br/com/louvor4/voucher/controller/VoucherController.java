package br.com.louvor4.voucher.controller;

import br.com.louvor4.voucher.dto.CreateVoucherRequest;
import br.com.louvor4.voucher.dto.RedeemVoucherRequest;
import br.com.louvor4.voucher.dto.VoucherResponse;
import br.com.louvor4.voucher.service.VoucherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("vouchers")
public class VoucherController {

    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @PostMapping
    public ResponseEntity<VoucherResponse> create(@RequestBody @Valid CreateVoucherRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(voucherService.create(request));
    }

    @PostMapping("/redeem")
    public ResponseEntity<Void> redeem(@RequestBody @Valid RedeemVoucherRequest request) {
        voucherService.redeem(request.code());
        return ResponseEntity.noContent().build();
    }
}
