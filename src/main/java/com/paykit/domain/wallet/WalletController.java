package com.paykit.domain.wallet;

import com.paykit.domain.wallet.dto.PayoutResponse;
import com.paykit.domain.wallet.dto.WalletResponse;
import com.paykit.domain.wallet.dto.WithdrawRequest;
import com.paykit.tenant.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/balance")
    public ResponseEntity<WalletResponse> getBalance() {
        UUID tenantId = TenantContext.get();
        return ResponseEntity.ok(walletService.getBalance(tenantId));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<PayoutResponse> withdraw(@Valid @RequestBody WithdrawRequest request) {
        UUID tenantId = TenantContext.get();
        return ResponseEntity.status(HttpStatus.CREATED).body(walletService.requestWithdrawal(tenantId, request));
    }

    @GetMapping("/payouts")
    public ResponseEntity<Page<PayoutResponse>> listPayouts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        UUID tenantId = TenantContext.get();
        return ResponseEntity.ok(walletService.listPayouts(tenantId,
                PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }
}
