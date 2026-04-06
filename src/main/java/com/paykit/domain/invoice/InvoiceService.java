package com.paykit.domain.invoice;

import com.paykit.domain.invoice.dto.CreateInvoiceRequest;
import com.paykit.domain.invoice.dto.InvoiceResponse;
import com.paykit.domain.invoice.dto.UpdateInvoiceStatusRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface InvoiceService {

    InvoiceResponse create(CreateInvoiceRequest request);

    InvoiceResponse getById(UUID id);

    InvoiceResponse getByInvoiceNumber(String invoiceNumber);

    Page<InvoiceResponse> list(InvoiceStatus status, Pageable pageable);

    InvoiceResponse updateStatus(UUID id, UpdateInvoiceStatusRequest request);
}
