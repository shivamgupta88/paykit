package com.paykit.domain.payment;

import com.paykit.domain.payment.dto.CreatePaymentRequest;
import com.paykit.domain.payment.dto.PaymentResponse;
import com.paykit.domain.payment.dto.VerifyPaymentRequest;

import java.util.List;
import java.util.UUID;

public interface PaymentService {

    PaymentResponse initiatePayment(CreatePaymentRequest request);

    PaymentResponse verifyPayment(VerifyPaymentRequest request);

    PaymentResponse getById(UUID id);

    List<PaymentResponse> listByInvoice(UUID invoiceId);

    void handleWebhookEvent(String eventType, String razorpayPaymentId, String razorpayOrderId);
}
