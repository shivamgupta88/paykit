package com.paykit.domain.wallet;

import com.paykit.domain.wallet.PayoutRequest.AccountType;
import com.paykit.domain.wallet.PayoutRequest.PayoutStatus;
import com.paykit.domain.wallet.dto.PayoutResponse;
import com.paykit.domain.wallet.dto.WalletResponse;
import com.paykit.domain.wallet.dto.WithdrawRequest;
import com.paykit.exception.PaymentProcessingException;
import com.paykit.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletServiceImpl implements WalletService {

    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.02"); // 2%

    private final TenantWalletRepository walletRepository;
    private final PayoutRequestRepository payoutRequestRepository;

    @Override
    @Transactional
    public void creditFromPayment(UUID tenantId, BigDecimal invoiceAmount) {
        BigDecimal commission = invoiceAmount.multiply(COMMISSION_RATE).setScale(4, RoundingMode.HALF_UP);
        BigDecimal credit = invoiceAmount.subtract(commission).setScale(4, RoundingMode.HALF_UP);

        TenantContext.set(tenantId);
        try {
            TenantWallet wallet = walletRepository.findByTenantIdForUpdate(tenantId)
                    .orElseGet(() -> {
                        TenantWallet w = new TenantWallet();
                        w.setBalance(BigDecimal.ZERO);
                        w.setTotalEarned(BigDecimal.ZERO);
                        w.setTotalWithdrawn(BigDecimal.ZERO);
                        return w;
                    });

            wallet.setBalance(wallet.getBalance().add(credit));
            wallet.setTotalEarned(wallet.getTotalEarned().add(credit));
            walletRepository.save(wallet);

            log.info("Wallet credited: tenantId={}, invoice={}, commission={}, credit={}",
                    tenantId, invoiceAmount, commission, credit);
        } finally {
            TenantContext.clear();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getBalance(UUID tenantId) {
        TenantWallet wallet = walletRepository.findByTenantId(tenantId)
                .orElse(null);

        if (wallet == null) {
            return WalletResponse.builder()
                    .balance(BigDecimal.ZERO)
                    .totalEarned(BigDecimal.ZERO)
                    .totalWithdrawn(BigDecimal.ZERO)
                    .build();
        }

        return WalletResponse.builder()
                .balance(wallet.getBalance())
                .totalEarned(wallet.getTotalEarned())
                .totalWithdrawn(wallet.getTotalWithdrawn())
                .build();
    }

    @Override
    @Transactional
    public PayoutResponse requestWithdrawal(UUID tenantId, WithdrawRequest request) {
        TenantWallet wallet = walletRepository.findByTenantIdForUpdate(tenantId)
                .orElseThrow(() -> new PaymentProcessingException("No wallet found. No payments received yet."));

        if (wallet.getBalance().compareTo(request.getAmount()) < 0) {
            throw new PaymentProcessingException(
                    "Insufficient balance. Available: ₹" + wallet.getBalance().setScale(2, RoundingMode.HALF_UP));
        }

        // Validate account details
        if (request.getAccountType() == AccountType.BANK) {
            if (request.getAccountNumber() == null || request.getIfscCode() == null
                    || request.getAccountHolderName() == null) {
                throw new PaymentProcessingException("Bank account details are incomplete.");
            }
        } else {
            if (request.getUpiId() == null || request.getUpiId().isBlank()) {
                throw new PaymentProcessingException("UPI ID is required.");
            }
        }

        // Deduct from wallet
        wallet.setBalance(wallet.getBalance().subtract(request.getAmount()));
        wallet.setTotalWithdrawn(wallet.getTotalWithdrawn().add(request.getAmount()));
        walletRepository.save(wallet);

        // Create payout request
        PayoutRequest payout = new PayoutRequest();
        payout.setAmount(request.getAmount());
        payout.setAccountType(request.getAccountType());
        payout.setAccountHolderName(request.getAccountHolderName());
        payout.setAccountNumber(request.getAccountNumber());
        payout.setIfscCode(request.getIfscCode());
        payout.setUpiId(request.getUpiId());
        payout.setStatus(PayoutStatus.PENDING);

        TenantContext.set(tenantId);
        try {
            payout = payoutRequestRepository.save(payout);
        } finally {
            TenantContext.clear();
        }

        log.info("Payout requested: tenantId={}, amount={}, type={}", tenantId, request.getAmount(), request.getAccountType());

        return toResponse(payout);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PayoutResponse> listPayouts(UUID tenantId, Pageable pageable) {
        return payoutRequestRepository
                .findAllByTenantIdOrderByCreatedAtDesc(tenantId, pageable)
                .map(this::toResponse);
    }

    private PayoutResponse toResponse(PayoutRequest p) {
        return PayoutResponse.builder()
                .id(p.getId())
                .amount(p.getAmount())
                .accountType(p.getAccountType())
                .accountHolderName(p.getAccountHolderName())
                .accountNumber(p.getAccountNumber())
                .ifscCode(p.getIfscCode())
                .upiId(p.getUpiId())
                .status(p.getStatus())
                .failureReason(p.getFailureReason())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
