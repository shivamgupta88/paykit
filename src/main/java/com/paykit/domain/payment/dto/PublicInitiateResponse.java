package com.paykit.domain.payment.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PublicInitiateResponse {
    private String razorpayOrderId;
    private long amountInPaise;
    private String currency;
    private String keyId;
}
