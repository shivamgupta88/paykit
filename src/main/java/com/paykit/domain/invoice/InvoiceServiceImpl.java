package com.paykit.domain.invoice;

import com.paykit.common.AppConstants;
import com.paykit.domain.customer.Customer;
import com.paykit.domain.customer.CustomerRepository;
import com.paykit.domain.email.EmailService;
import com.paykit.domain.invoice.dto.CreateInvoiceItemRequest;
import com.paykit.domain.invoice.dto.CreateInvoiceRequest;
import com.paykit.domain.invoice.dto.InvoiceResponse;
import com.paykit.domain.invoice.dto.UpdateInvoiceStatusRequest;
import com.paykit.exception.InvalidStateTransitionException;
import com.paykit.exception.ResourceNotFoundException;
import com.paykit.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoiceServiceImpl implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;
    private final InvoiceMapper invoiceMapper;
    private final EmailService emailService;

    private static final Map<InvoiceStatus, Set<InvoiceStatus>> TRANSITIONS = new EnumMap<>(InvoiceStatus.class);

    static {
        TRANSITIONS.put(InvoiceStatus.DRAFT, Set.of(InvoiceStatus.SENT, InvoiceStatus.CANCELLED));
        TRANSITIONS.put(InvoiceStatus.SENT, Set.of(InvoiceStatus.PAID, InvoiceStatus.OVERDUE, InvoiceStatus.CANCELLED));
        TRANSITIONS.put(InvoiceStatus.OVERDUE, Set.of(InvoiceStatus.PAID, InvoiceStatus.CANCELLED));
        TRANSITIONS.put(InvoiceStatus.PAID, Set.of());
        TRANSITIONS.put(InvoiceStatus.CANCELLED, Set.of());
    }

    @Override
    @Transactional
    public InvoiceResponse create(CreateInvoiceRequest request) {
        UUID tenantId = TenantContext.get();

        Customer customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new ResourceNotFoundException("Customer", request.getCustomerId()));
        if (!customer.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Customer", request.getCustomerId());
        }

        Invoice invoice = new Invoice();
        invoice.setInvoiceNumber(generateInvoiceNumber());
        invoice.setCustomerId(request.getCustomerId());
        invoice.setStatus(InvoiceStatus.DRAFT);
        invoice.setIssueDate(request.getIssueDate());
        invoice.setDueDate(request.getDueDate());
        invoice.setCurrency(request.getCurrency());
        invoice.setNotes(request.getNotes());

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;

        for (CreateInvoiceItemRequest itemReq : request.getItems()) {
            InvoiceItem item = new InvoiceItem();
            item.setDescription(itemReq.getDescription());
            item.setQuantity(itemReq.getQuantity());
            item.setUnitPrice(itemReq.getUnitPrice());
            item.setTaxRate(itemReq.getTaxRate());

            BigDecimal lineBase = itemReq.getQuantity().multiply(itemReq.getUnitPrice()).setScale(4, RoundingMode.HALF_UP);
            BigDecimal lineTax = lineBase.multiply(itemReq.getTaxRate()).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
            item.setLineTotal(lineBase.add(lineTax));

            item.setInvoice(invoice);
            item.setTenantId(tenantId);
            invoice.getItems().add(item);

            subtotal = subtotal.add(lineBase);
            taxAmount = taxAmount.add(lineTax);
        }

        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalAmount(subtotal.add(taxAmount));

        invoice = invoiceRepository.save(invoice);
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getById(UUID id) {
        Invoice invoice = findByIdAndTenant(id);
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public InvoiceResponse getByInvoiceNumber(String invoiceNumber) {
        UUID tenantId = TenantContext.get();
        Invoice invoice = invoiceRepository.findByTenantIdAndInvoiceNumber(tenantId, invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceNumber));
        return invoiceMapper.toResponse(invoice);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InvoiceResponse> list(InvoiceStatus status, Pageable pageable) {
        UUID tenantId = TenantContext.get();
        Page<Invoice> page;
        if (status != null) {
            page = invoiceRepository.findAllByTenantIdAndStatus(tenantId, status, pageable);
        } else {
            page = invoiceRepository.findAllByTenantId(tenantId, pageable);
        }
        return page.map(invoiceMapper::toResponse);
    }

    @Override
    @Transactional
    public InvoiceResponse updateStatus(UUID id, UpdateInvoiceStatusRequest request) {
        Invoice invoice = findByIdAndTenant(id);
        InvoiceStatus current = invoice.getStatus();
        InvoiceStatus target = request.getStatus();

        Set<InvoiceStatus> allowed = TRANSITIONS.getOrDefault(current, Set.of());
        if (!allowed.contains(target)) {
            throw new InvalidStateTransitionException(current.name(), target.name());
        }

        invoice.setStatus(target);
        invoice = invoiceRepository.save(invoice);

        if (target == InvoiceStatus.SENT) {
            Customer customer = customerRepository.findById(invoice.getCustomerId()).orElse(null);
            if (customer != null) {
                emailService.sendInvoiceEmail(
                        customer.getEmail(),
                        "Invoice " + invoice.getInvoiceNumber() + " from PayKit",
                        "You have received invoice " + invoice.getInvoiceNumber()
                                + ". Amount due: " + invoice.getCurrency() + " " + invoice.getTotalAmount()
                                + ". Due date: " + invoice.getDueDate()
                );
            }
        }

        return invoiceMapper.toResponse(invoice);
    }

    private Invoice findByIdAndTenant(UUID id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", id));
        if (!invoice.getTenantId().equals(TenantContext.get())) {
            throw new ResourceNotFoundException("Invoice", id);
        }
        return invoice;
    }

    private String generateInvoiceNumber() {
        return AppConstants.INVOICE_PREFIX + UUID.randomUUID().toString().substring(0, 8);
    }
}
