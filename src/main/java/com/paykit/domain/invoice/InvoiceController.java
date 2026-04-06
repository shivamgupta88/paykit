package com.paykit.domain.invoice;

import com.paykit.domain.invoice.dto.CreateInvoiceRequest;
import com.paykit.domain.invoice.dto.InvoiceResponse;
import com.paykit.domain.invoice.dto.UpdateInvoiceStatusRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

import static com.paykit.common.AppConstants.DEFAULT_PAGE_SIZE;
import static com.paykit.common.AppConstants.MAX_PAGE_SIZE;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<InvoiceResponse> create(@Valid @RequestBody CreateInvoiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(invoiceService.create(request));
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(invoiceService.getById(id));
    }

    @GetMapping("/number/{invoiceNumber}")
    public ResponseEntity<InvoiceResponse> getByInvoiceNumber(@PathVariable String invoiceNumber) {
        return ResponseEntity.ok(invoiceService.getByInvoiceNumber(invoiceNumber));
    }

    @GetMapping
    public ResponseEntity<Page<InvoiceResponse>> list(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        size = Math.min(size, MAX_PAGE_SIZE);
        return ResponseEntity.ok(invoiceService.list(status, PageRequest.of(page, size, Sort.by("createdAt").descending())));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<InvoiceResponse> updateStatus(@PathVariable UUID id,
                                                        @Valid @RequestBody UpdateInvoiceStatusRequest request) {
        return ResponseEntity.ok(invoiceService.updateStatus(id, request));
    }
}
