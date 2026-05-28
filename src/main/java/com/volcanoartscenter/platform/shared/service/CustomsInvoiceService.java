package com.volcanoartscenter.platform.shared.service;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.volcanoartscenter.platform.shared.model.OrderItem;
import com.volcanoartscenter.platform.shared.model.ShippingOrder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Generates a printable commercial invoice (customs declaration) for a
 * cross-border ShippingOrder. Uses iText 9. The PDF body lists every
 * OrderItem with declared unit value, quantity, line total, and currency.
 */
@Service
@Slf4j
public class CustomsInvoiceService {

    @Value("${platform.organization.name:Volcano Arts Center Inc.}")
    private String orgName;

    @Value("${platform.organization.address:Musanze, Rwanda}")
    private String orgAddress;

    @Value("${platform.organization.tin:}")
    private String orgTin;

    public byte[] generate(ShippingOrder order) {
        if (order == null) throw new IllegalArgumentException("ShippingOrder is required");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdfDoc = new PdfDocument(writer);
             Document doc = new Document(pdfDoc, PageSize.A4)) {

            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont italic = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            doc.add(new Paragraph("Commercial Invoice")
                    .setFont(bold).setFontSize(18).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Order Reference: " + safe(order.getOrderReference()))
                    .setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph("Issued: " + LocalDate.now())
                    .setTextAlignment(TextAlignment.CENTER));

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Shipper").setFont(bold));
            doc.add(new Paragraph(orgName));
            doc.add(new Paragraph(orgAddress));
            if (orgTin != null && !orgTin.isBlank()) {
                doc.add(new Paragraph("TIN: " + orgTin));
            }

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Recipient").setFont(bold));
            doc.add(new Paragraph(safe(order.getRecipientName())));
            doc.add(new Paragraph(safe(order.getAddressLine1())));
            if (order.getAddressLine2() != null && !order.getAddressLine2().isBlank()) {
                doc.add(new Paragraph(order.getAddressLine2()));
            }
            doc.add(new Paragraph(joinAddress(order)));
            doc.add(new Paragraph("Email: " + safe(order.getRecipientEmail())));
            if (order.getRecipientPhone() != null) {
                doc.add(new Paragraph("Phone: " + order.getRecipientPhone()));
            }

            doc.add(new Paragraph(" "));

            Table table = new Table(UnitValue.createPercentArray(new float[]{6f, 1f, 2f, 2f, 1f}))
                    .useAllAvailableWidth();
            table.addHeaderCell(headerCell("Description", bold));
            table.addHeaderCell(headerCell("Qty", bold));
            table.addHeaderCell(headerCell("Unit", bold));
            table.addHeaderCell(headerCell("Line Total", bold));
            table.addHeaderCell(headerCell("Cur.", bold));

            String currency = safe(order.getCurrency(), "USD");
            List<OrderItem> items = order.getOrderItems() == null ? List.of() : order.getOrderItems();
            BigDecimal goodsTotal = BigDecimal.ZERO;

            if (!items.isEmpty()) {
                for (OrderItem it : items) {
                    table.addCell(safe(it.getProductName()));
                    table.addCell(String.valueOf(it.getQuantity()));
                    table.addCell(it.getPriceAtPurchase() == null ? "-" : it.getPriceAtPurchase().toPlainString());
                    BigDecimal line = it.getLineTotal() == null ? BigDecimal.ZERO : it.getLineTotal();
                    goodsTotal = goodsTotal.add(line);
                    table.addCell(line.toPlainString());
                    table.addCell(currency);
                }
            } else if (order.getProduct() != null) {
                String name = order.getProduct().getName();
                BigDecimal unit = order.getProduct().getPrice();
                int qty = order.getQuantity() == null ? 1 : order.getQuantity();
                BigDecimal line = (unit == null ? BigDecimal.ZERO : unit).multiply(BigDecimal.valueOf(qty));
                goodsTotal = goodsTotal.add(line);
                table.addCell(safe(name));
                table.addCell(String.valueOf(qty));
                table.addCell(unit == null ? "-" : unit.toPlainString());
                table.addCell(line.toPlainString());
                table.addCell(currency);
            }

            doc.add(table);

            BigDecimal shipping = order.getShippingCost() == null ? BigDecimal.ZERO : order.getShippingCost();
            BigDecimal grand = order.getTotalAmount() == null ? goodsTotal.add(shipping) : order.getTotalAmount();

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Goods total: " + goodsTotal.toPlainString() + " " + currency)
                    .setTextAlignment(TextAlignment.RIGHT));
            doc.add(new Paragraph("Shipping: " + shipping.toPlainString() + " " + currency)
                    .setTextAlignment(TextAlignment.RIGHT));
            doc.add(new Paragraph("Grand total: " + grand.toPlainString() + " " + currency)
                    .setFont(bold).setTextAlignment(TextAlignment.RIGHT));

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Country of origin: Rwanda. Reason for export: Sale of goods.")
                    .setFont(italic).setFontSize(10));
            doc.add(new Paragraph("I hereby certify that the information on this invoice is true and correct.")
                    .setFont(italic).setFontSize(10));
            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Signature: ____________________________   Date: " + LocalDate.now())
                    .setFontSize(10));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to render customs invoice PDF", ex);
        }
        log.info("Generated customs invoice PDF for order {} ({} bytes)",
                order.getOrderReference(), baos.size());
        return baos.toByteArray();
    }

    private Cell headerCell(String text, PdfFont bold) {
        return new Cell().add(new Paragraph(text).setFont(bold));
    }

    private String joinAddress(ShippingOrder o) {
        StringBuilder sb = new StringBuilder();
        if (o.getCity() != null) sb.append(o.getCity());
        if (o.getState() != null && !o.getState().isBlank()) sb.append(", ").append(o.getState());
        if (o.getPostalCode() != null && !o.getPostalCode().isBlank()) sb.append(" ").append(o.getPostalCode());
        if (o.getCountry() != null) sb.append(", ").append(o.getCountry());
        return sb.toString();
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static String safe(String s, String fallback) { return (s == null || s.isBlank()) ? fallback : s; }
}
