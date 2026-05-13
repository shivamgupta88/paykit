package com.paykit.domain.wallet;

import com.paykit.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "payout_requests")
@Getter
@Setter
public class PayoutRequest extends BaseEntity {

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(name = "account_type", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    private AccountType accountType;

    @Column(name = "account_holder_name")
    private String accountHolderName;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "ifsc_code", length = 20)
    private String ifscCode;

    @Column(name = "upi_id", length = 100)
    private String upiId;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private PayoutStatus status = PayoutStatus.PENDING;

    @Column(name = "razorpay_payout_id", length = 100)
    private String razorpayPayoutId;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    public enum AccountType { BANK, UPI }

    public enum PayoutStatus { PENDING, PROCESSING, SUCCESS, FAILED }
}
