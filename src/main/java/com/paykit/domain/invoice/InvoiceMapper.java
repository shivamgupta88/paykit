package com.paykit.domain.invoice;

import com.paykit.domain.invoice.dto.InvoiceItemResponse;
import com.paykit.domain.invoice.dto.InvoiceResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    InvoiceResponse toResponse(Invoice invoice);

    InvoiceItemResponse toItemResponse(InvoiceItem item);
}
