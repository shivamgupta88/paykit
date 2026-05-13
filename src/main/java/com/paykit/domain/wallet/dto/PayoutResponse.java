package com.paykit.domain.wallet.dto;

import com.paykit.domain.wallet.PayoutRequest.AccountType;
import com.paykit.domain.wallet.PayoutRequest.PayoutStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Builder
public class PayoutResponse {
    private UUID id;
    private BigDecimal amount;
    private AccountType accountType;
    private String accountHolderName;
    private String accountNumber;
    private String ifscCode;
    private String upiId;
    private PayoutStatus status;
    private String failureReason;
    private Instant createdAt;
}
