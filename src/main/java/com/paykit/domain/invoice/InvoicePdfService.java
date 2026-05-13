package com.paykit.domain.invoice;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Text;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.paykit.domain.customer.Customer;
import com.paykit.domain.customer.CustomerRepository;
import com.paykit.exception.ResourceNotFoundException;
import com.paykit.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private final InvoiceRepository invoiceRepository;
    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public byte[] generatePdf(UUID invoiceId) {
        UUID tenantId = TenantContext.get();
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId));
        if (!invoice.getTenantId().equals(tenantId)) {
            throw new ResourceNotFoundException("Invoice", invoiceId);
        }

        Customer customer = customerRepository.findById(invoice.getCustomerId()).orElse(null);

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            DeviceRgb indigo = new DeviceRgb(79, 70, 229);
            DeviceRgb dark = new DeviceRgb(15, 23, 42);
            DeviceRgb muted = new DeviceRgb(100, 116, 139);
            DeviceRgb headerBg = new DeviceRgb(241, 245, 249);

            // Header — PayKit branding
            doc.add(new Paragraph()
                    .add(new Text("Pay").setFont(bold).setFontColor(dark).setFontSize(20))
                    .add(new Text("Kit").setFont(bold).setFontColor(indigo).setFontSize(20)));

            doc.add(new Paragraph("Invoice & Payment Platform")
                    .setFont(regular).setFontSize(10).setFontColor(muted).setMarginTop(2));

            doc.add(new Paragraph(" "));

            // Invoice metadata
            doc.add(new Paragraph(invoice.getInvoiceNumber())
                    .setFont(bold).setFontSize(18).setFontColor(dark));
            doc.add(new Paragraph(
                    "Issue Date: " + invoice.getIssueDate() + "    Due Date: " + invoice.getDueDate()
                            + "    Currency: " + invoice.getCurrency())
                    .setFont(regular).setFontSize(10).setFontColor(muted));

            doc.add(new Paragraph(" "));

            // Bill To
            doc.add(new Paragraph("BILL TO")
                    .setFont(bold).setFontSize(9).setFontColor(muted));

            if (customer != null) {
                doc.add(new Paragraph(customer.getName())
                        .setFont(bold).setFontSize(13).setFontColor(dark).setMarginTop(2));
                if (customer.getEmail() != null) {
                    doc.add(new Paragraph(customer.getEmail())
                            .setFont(regular).setFontSize(10).setFontColor(muted));
                }
                if (customer.getPhone() != null) {
                    doc.add(new Paragraph(customer.getPhone())
                            .setFont(regular).setFontSize(10).setFontColor(muted));
                }
                if (customer.getBillingAddress() != null) {
                    doc.add(new Paragraph(customer.getBillingAddress())
                            .setFont(regular).setFontSize(10).setFontColor(muted));
                }
                if (customer.getGstin() != null) {
                    doc.add(new Paragraph("GSTIN: " + customer.getGstin())
                            .setFont(regular).setFontSize(10).setFontColor(muted));
                }
            } else {
                doc.add(new Paragraph("—").setFont(regular).setFontSize(12).setFontColor(muted));
            }

            doc.add(new Paragraph(" "));

            // Line items table
            Table table = new Table(UnitValue.createPercentArray(new float[]{5, 1.2f, 1.8f, 1f, 1.8f}))
                    .setWidth(UnitValue.createPercentValue(100));

            String[] colHeaders = {"Description", "Qty", "Unit Price", "Tax", "Amount"};
            TextAlignment[] aligns = {
                    TextAlignment.LEFT, TextAlignment.RIGHT, TextAlignment.RIGHT,
                    TextAlignment.RIGHT, TextAlignment.RIGHT
            };
            for (int i = 0; i < colHeaders.length; i++) {
                table.addHeaderCell(new Cell()
                        .add(new Paragraph(colHeaders[i]).setFont(bold).setFontSize(9).setFontColor(muted))
                        .setBackgroundColor(headerBg)
                        .setBorder(Border.NO_BORDER)
                        .setTextAlignment(aligns[i])
                        .setPadding(7));
            }

            for (InvoiceItem item : invoice.getItems()) {
                addCell(table, item.getDescription(), regular, dark, TextAlignment.LEFT);
                addCell(table, stripZeros(item.getQuantity()), regular, muted, TextAlignment.RIGHT);
                addCell(table, fmtCurrency(item.getUnitPrice(), invoice.getCurrency()), regular, muted, TextAlignment.RIGHT);
                addCell(table, stripZeros(item.getTaxRate()) + "%", regular, muted, TextAlignment.RIGHT);
                addCell(table, fmtCurrency(item.getLineTotal(), invoice.getCurrency()), bold, dark, TextAlignment.RIGHT);
            }

            doc.add(table);
            doc.add(new Paragraph(" "));

            // Totals
            Table totals = new Table(UnitValue.createPercentArray(new float[]{5, 2}))
                    .setWidth(UnitValue.createPercentValue(100));

            addTotalRow(totals, "Subtotal", fmtCurrency(invoice.getSubtotal(), invoice.getCurrency()),
                    regular, muted, dark);
            addTotalRow(totals, "Tax", fmtCurrency(invoice.getTaxAmount(), invoice.getCurrency()),
                    regular, muted, dark);
            addTotalRow(totals, "Total Due", fmtCurrency(invoice.getTotalAmount(), invoice.getCurrency()),
                    bold, dark, dark);

            doc.add(totals);

            if (invoice.getNotes() != null && !invoice.getNotes().isBlank()) {
                doc.add(new Paragraph(" "));
                doc.add(new Paragraph("NOTES").setFont(bold).setFontSize(9).setFontColor(muted));
                doc.add(new Paragraph(invoice.getNotes()).setFont(regular).setFontSize(10).setFontColor(muted));
            }

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Generated by PayKit")
                    .setFont(regular).setFontSize(9).setFontColor(muted)
                    .setTextAlignment(TextAlignment.CENTER));

            doc.close();
            return baos.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Failed to generate PDF for invoice " + invoiceId, e);
        }
    }

    private void addCell(Table table, String text, PdfFont font, DeviceRgb color, TextAlignment align) {
        table.addCell(new Cell()
                .add(new Paragraph(text == null ? "" : text).setFont(font).setFontSize(10).setFontColor(color))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(align)
                .setPadding(7));
    }

    private void addTotalRow(Table table, String label, String value,
                             PdfFont font, DeviceRgb labelColor, DeviceRgb valueColor) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setFont(font).setFontSize(11).setFontColor(labelColor))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(5));
        table.addCell(new Cell()
                .add(new Paragraph(value).setFont(font).setFontSize(11).setFontColor(valueColor))
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(5));
    }

    private String fmtCurrency(BigDecimal amount, String currency) {
        if (amount == null) return currency + " 0.00";
        return currency + " " + amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String stripZeros(BigDecimal val) {
        if (val == null) return "0";
        return val.stripTrailingZeros().toPlainString();
    }
}
