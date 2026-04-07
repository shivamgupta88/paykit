package com.paykit.domain.payment;

import com.paykit.domain.customer.Customer;
import com.paykit.domain.customer.CustomerRepository;
import com.paykit.domain.email.EmailService;
import com.paykit.domain.invoice.Invoice;
import com.paykit.domain.invoice.InvoiceRepository;
import com.paykit.domain.invoice.InvoiceStatus;
import com.paykit.domain.payment.dto.CreatePaymentRequest;
import com.paykit.domain.payment.dto.PaymentResponse;
import com.paykit.domain.payment.dto.VerifyPaymentRequest;
import com.paykit.exception.PaymentProcessingException;
import com.paykit.exception.ResourceNotFoundException;
import com.paykit.tenant.TenantContext;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final RazorpayClient razorpayClient;
    private final RazorpayProperties razorpayProperties;
    private final PaymentMapper paymentMapper;
    private final EmailService emailService;

    @Override
    @Transactional
    public PaymentResponse initiatePayment(CreatePaymentRequest request) {
        UUID tenantId = TenantContext.get();

        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", request.getInvoiceId()));
        if (!invoice.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Invoice", request.getInvoiceId());
        }
        if (invoice.getStatus() != InvoiceStatus.SENT && invoice.getStatus() != InvoiceStatus.OVERDUE) {
            throw new PaymentProcessingException("Invoice must be in SENT or OVERDUE status to initiate payment");
        }

        try {
            JSONObject options = new JSONObject();
            options.put("amount", invoice.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue());
            options.put("currency", invoice.getCurrency());
            options.put("receipt", invoice.getInvoiceNumber());

            Order razorpayOrder = razorpayClient.orders.create(options);

            Payment payment = new Payment();
            payment.setInvoiceId(invoice.getId());
            payment.setRazorpayOrderId(razorpayOrder.get("id"));
            payment.setAmount(invoice.getTotalAmount());
            payment.setCurrency(invoice.getCurrency());
            payment.setStatus(PaymentStatus.INITIATED);

            payment = paymentRepository.save(payment);
            return paymentMapper.toResponse(payment);
        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for invoice {}", invoice.getInvoiceNumber(), e);
            throw new PaymentProcessingException("Failed to create payment order", e);
        }
    }

    @Override
    @Transactional
    public PaymentResponse verifyPayment(VerifyPaymentRequest request) {
        UUID tenantId = TenantContext.get();

        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", request.getRazorpayOrderId());
            attributes.put("razorpay_payment_id", request.getRazorpayPaymentId());
            attributes.put("razorpay_signature", request.getRazorpaySignature());
            Utils.verifyPaymentSignature(attributes, razorpayProperties.getKeySecret());
        } catch (RazorpayException e) {
            throw new PaymentProcessingException("Payment signature verification failed");
        }

        Payment payment = paymentRepository.findByTenantIdAndRazorpayPaymentId(tenantId, request.getRazorpayPaymentId())
                .orElseGet(() -> {
                    Payment p = new Payment();
                    p.setRazorpayOrderId(request.getRazorpayOrderId());
                    p.setRazorpayPaymentId(request.getRazorpayPaymentId());
                    return p;
                });

        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            return paymentMapper.toResponse(payment);
        }

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setCapturedAt(Instant.now());
        payment = paymentRepository.save(payment);

        markInvoicePaid(payment.getInvoiceId(), tenantId);

        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public PaymentResponse getById(UUID id) {
        UUID tenantId = TenantContext.get();
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", id));
        if (!payment.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Payment", id);
        }
        return paymentMapper.toResponse(payment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentResponse> listByInvoice(UUID invoiceId) {
        return paymentRepository.findAllByInvoiceId(invoiceId).stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void handleWebhookEvent(String eventType, String razorpayPaymentId, String razorpayOrderId) {
        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
        if (payment == null) {
            log.warn("No payment found for Razorpay order {}", razorpayOrderId);
            return;
        }

        if ("payment.captured".equals(eventType)) {
            if (payment.getStatus() == PaymentStatus.CAPTURED) {
                return;
            }
            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setStatus(PaymentStatus.CAPTURED);
            payment.setCapturedAt(Instant.now());
            paymentRepository.save(payment);
            markInvoicePaid(payment.getInvoiceId(), payment.getTenantId());

        } else if ("payment.failed".equals(eventType)) {
            if (payment.getStatus() == PaymentStatus.CAPTURED) {
                return;
            }
            payment.setRazorpayPaymentId(razorpayPaymentId);
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason("Payment failed via webhook");
            paymentRepository.save(payment);
        }
    }

    private void markInvoicePaid(UUID invoiceId, UUID tenantId) {
        Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
        if (invoice == null || invoice.getStatus() == InvoiceStatus.PAID) {
            return;
        }
        invoice.setStatus(InvoiceStatus.PAID);
        invoiceRepository.save(invoice);

        Customer customer = customerRepository.findById(invoice.getCustomerId()).orElse(null);
        if (customer != null) {
            emailService.sendInvoiceEmail(
                    customer.getEmail(),
                    "Payment received for " + invoice.getInvoiceNumber(),
                    "Payment has been successfully captured for invoice " + invoice.getInvoiceNumber()
                            + ". Amount: " + invoice.getCurrency() + " " + invoice.getTotalAmount()
            );
        }
    }
}
