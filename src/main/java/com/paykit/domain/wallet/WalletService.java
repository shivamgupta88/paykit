package com.paykit.domain.wallet;

import com.paykit.domain.wallet.dto.PayoutResponse;
import com.paykit.domain.wallet.dto.WalletResponse;
import com.paykit.domain.wallet.dto.WithdrawRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletService {

    /** Credit wallet after a payment is captured. Commission is deducted automatically. */
    void creditFromPayment(UUID tenantId, BigDecimal invoiceAmount);

    WalletResponse getBalance(UUID tenantId);

    PayoutResponse requestWithdrawal(UUID tenantId, WithdrawRequest request);

    Page<PayoutResponse> listPayouts(UUID tenantId, Pageable pageable);
}
