package com.paykit.domain.payment.dto;

import com.paykit.domain.invoice.InvoiceStatus;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
public class PublicInvoiceResponse {
    private UUID invoiceId;
    private String invoiceNumber;
    private String customerName;
    private BigDecimal amount;
    private String currency;
    private InvoiceStatus status;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private String notes;
}
