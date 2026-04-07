package com.paykit.domain.payment.dto;

import com.paykit.domain.payment.PaymentStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
public class PaymentResponse {

    private UUID id;
    private UUID invoiceId;
    private String razorpayOrderId;
    private String razorpayPaymentId;
    private BigDecimal amount;
    private String currency;
    private PaymentStatus status;
    private String failureReason;
    private Instant capturedAt;
    private Instant createdAt;
    private Instant updatedAt;
}
