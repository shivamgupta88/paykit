package com.paykit.domain.invoice.dto;

import com.paykit.domain.invoice.InvoiceStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class InvoiceResponse {

    private UUID id;
    private String invoiceNumber;
    private UUID customerId;
    private InvoiceStatus status;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String currency;
    private String notes;
    private List<InvoiceItemResponse> items;
    private Instant createdAt;
    private Instant updatedAt;
}
