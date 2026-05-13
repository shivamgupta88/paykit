package com.paykit.domain.payment;

import com.paykit.domain.customer.Customer;
import com.paykit.domain.customer.CustomerRepository;
import com.paykit.domain.invoice.Invoice;
import com.paykit.domain.invoice.InvoiceRepository;
import com.paykit.domain.invoice.InvoiceStatus;
import com.paykit.domain.payment.dto.PublicInitiateResponse;
import com.paykit.domain.payment.dto.PublicInvoiceResponse;
import com.paykit.domain.payment.dto.VerifyPaymentRequest;
import com.paykit.domain.wallet.WalletService;
import com.paykit.exception.PaymentProcessingException;
import com.paykit.exception.ResourceNotFoundException;
import com.paykit.tenant.TenantContext;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicPaymentController {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final PaymentRepository paymentRepository;
    private final RazorpayClient razorpayClient;
    private final RazorpayProperties razorpayProperties;
    private final WalletService walletService;

    @GetMapping("/invoices/{id}")
    public ResponseEntity<PublicInvoiceResponse> getInvoice(@PathVariable UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));

        if (invoice.getStatus() != InvoiceStatus.SENT && invoice.getStatus() != InvoiceStatus.OVERDUE) {
            throw new ResourceNotFoundException("Invoice", id);
        }

        Customer customer = customerRepository.findById(invoice.getCustomerId()).orElse(null);

        return ResponseEntity.ok(PublicInvoiceResponse.builder()
                .invoiceId(invoice.getId())
                .invoiceNumber(invoice.getInvoiceNumber())
                .customerName(customer != null ? customer.getName() : null)
                .amount(invoice.getTotalAmount())
                .currency(invoice.getCurrency())
                .status(invoice.getStatus())
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .notes(invoice.getNotes())
                .build());
    }

    @PostMapping("/payments/initiate")
    public ResponseEntity<PublicInitiateResponse> initiate(@RequestParam UUID invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));

        if (invoice.getStatus() != InvoiceStatus.SENT && invoice.getStatus() != InvoiceStatus.OVERDUE) {
            throw new PaymentProcessingException("Invoice is not payable");
        }

        try {
            long amountInPaise = invoice.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();

            JSONObject options = new JSONObject();
            options.put("amount", amountInPaise);
            options.put("currency", invoice.getCurrency());
            options.put("receipt", invoice.getInvoiceNumber());

            Order razorpayOrder = razorpayClient.orders.create(options);
            String orderId = razorpayOrder.get("id");

            TenantContext.set(invoice.getTenantId());
            try {
                Payment payment = new Payment();
                payment.setInvoiceId(invoice.getId());
                payment.setRazorpayOrderId(orderId);
                payment.setAmount(invoice.getTotalAmount());
                payment.setCurrency(invoice.getCurrency());
                payment.setStatus(PaymentStatus.INITIATED);
                paymentRepository.save(payment);
            } finally {
                TenantContext.clear();
            }

            return ResponseEntity.ok(PublicInitiateResponse.builder()
                    .razorpayOrderId(orderId)
                    .amountInPaise(amountInPaise)
                    .currency(invoice.getCurrency())
                    .keyId(razorpayProperties.getKeyId())
                    .build());

        } catch (RazorpayException e) {
            log.error("Failed to create Razorpay order for public payment, invoiceId={}", invoiceId, e);
            throw new PaymentProcessingException("Failed to create payment order");
        }
    }

    @PostMapping("/payments/verify")
    public ResponseEntity<Void> verify(@Valid @RequestBody VerifyPaymentRequest request) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", request.getRazorpayOrderId());
            attributes.put("razorpay_payment_id", request.getRazorpayPaymentId());
            attributes.put("razorpay_signature", request.getRazorpaySignature());
            Utils.verifyPaymentSignature(attributes, razorpayProperties.getKeySecret());
        } catch (RazorpayException e) {
            throw new PaymentProcessingException("Payment signature verification failed");
        }

        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment", request.getRazorpayOrderId()));

        if (payment.getStatus() == PaymentStatus.CAPTURED) {
            return ResponseEntity.ok().build();
        }

        payment.setRazorpayPaymentId(request.getRazorpayPaymentId());
        payment.setStatus(PaymentStatus.CAPTURED);
        payment.setCapturedAt(Instant.now());

        TenantContext.set(payment.getTenantId());
        try {
            paymentRepository.save(payment);

            Invoice invoice = invoiceRepository.findById(payment.getInvoiceId()).orElse(null);
            if (invoice != null && invoice.getStatus() != InvoiceStatus.PAID) {
                invoice.setStatus(InvoiceStatus.PAID);
                invoiceRepository.save(invoice);
                walletService.creditFromPayment(payment.getTenantId(), invoice.getTotalAmount());
            }
        } finally {
            TenantContext.clear();
        }

        return ResponseEntity.ok().build();
    }
}
