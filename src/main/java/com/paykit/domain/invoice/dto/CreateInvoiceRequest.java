package com.paykit.domain.invoice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
public class CreateInvoiceRequest {

    @NotNull
    private UUID customerId;

    @NotNull
    private LocalDate issueDate;

    @NotNull
    private LocalDate dueDate;

    @NotNull
    private String currency;

    private String notes;

    @NotEmpty
    @Valid
    private List<CreateInvoiceItemRequest> items;
}
