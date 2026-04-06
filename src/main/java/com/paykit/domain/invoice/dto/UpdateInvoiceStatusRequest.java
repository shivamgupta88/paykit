package com.paykit.domain.invoice.dto;

import com.paykit.domain.invoice.InvoiceStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateInvoiceStatusRequest {

    @NotNull
    private InvoiceStatus status;
}
