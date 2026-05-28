package com.volcanoartscenter.platform.shared.donation;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.volcanoartscenter.platform.shared.model.Donation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Generates a printable donation receipt PDF (one page, A4) for a single
 * Donation. Includes VAC reference, donor info, amount, currency, impact tier
 * label, and an acknowledgement line for tax/record-keeping purposes.
 */
@Service
@Slf4j
public class DonationReceiptService {

    @Value("${platform.organization.name:Volcano Arts Center Inc.}")
    private String orgName;

    @Value("${platform.organization.address:Musanze, Rwanda}")
    private String orgAddress;

    @Value("${platform.organization.tin:}")
    private String orgTin;

    public byte[] generate(Donation donation) {
        if (donation == null) throw new IllegalArgumentException("Donation is required");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (PdfWriter writer = new PdfWriter(baos);
             PdfDocument pdf = new PdfDocument(writer);
             Document doc = new Document(pdf, PageSize.A4)) {

            PdfFont bold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont italic = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            doc.add(new Paragraph("Donation Receipt")
                    .setFont(bold).setFontSize(20).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph(orgName).setTextAlignment(TextAlignment.CENTER));
            doc.add(new Paragraph(orgAddress).setTextAlignment(TextAlignment.CENTER));
            if (orgTin != null && !orgTin.isBlank()) {
                doc.add(new Paragraph("TIN: " + orgTin).setTextAlignment(TextAlignment.CENTER));
            }

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Receipt: " + safe(donation.getReference())).setFont(bold));
            doc.add(new Paragraph("Issued: " + LocalDate.now().format(DateTimeFormatter.ISO_DATE)));

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Donor").setFont(bold));
            doc.add(new Paragraph(Boolean.TRUE.equals(donation.getIsAnonymous())
                    ? "Anonymous donor"
                    : safe(donation.getDonorName())));
            if (!Boolean.TRUE.equals(donation.getIsAnonymous())) {
                doc.add(new Paragraph("Email: " + safe(donation.getDonorEmail())));
                if (donation.getDonorCountry() != null && !donation.getDonorCountry().isBlank()) {
                    doc.add(new Paragraph("Country: " + donation.getDonorCountry()));
                }
            }

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Amount").setFont(bold));
            doc.add(new Paragraph(donation.getAmount().toPlainString() + " " + safe(donation.getCurrency(), "USD")));
            if (donation.getCampaign() != null) {
                doc.add(new Paragraph("Campaign: " + donation.getCampaign().getName()));
            }
            if (donation.getPurpose() != null) {
                doc.add(new Paragraph("Purpose: " + donation.getPurpose().name().replace('_', ' ')));
            }
            if (Boolean.TRUE.equals(donation.getIsRecurring())) {
                doc.add(new Paragraph("Recurring: " + (donation.getRecurringFrequency() == null
                        ? "MONTHLY" : donation.getRecurringFrequency().name())));
            }
            if (donation.getImpactTierLabel() != null && !donation.getImpactTierLabel().isBlank()) {
                doc.add(new Paragraph("Impact: " + donation.getImpactTierLabel()).setFont(italic));
            }

            doc.add(new Paragraph(" "));
            doc.add(new Paragraph("Thank you for your generous support of the arts and culture of Rwanda.")
                    .setFont(italic));
            doc.add(new Paragraph("Please retain this receipt for your records.")
                    .setFont(italic).setFontSize(10));
            if (donation.getMessage() != null && !donation.getMessage().isBlank()) {
                doc.add(new Paragraph(" "));
                doc.add(new Paragraph("Donor message: " + donation.getMessage()).setFontSize(10));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to render donation receipt PDF", ex);
        }
        log.info("Generated donation receipt PDF for {} ({} bytes)",
                donation.getReference(), baos.size());
        return baos.toByteArray();
    }

    private static String safe(String s) { return s == null ? "" : s; }
    private static String safe(String s, String fallback) {
        return (s == null || s.isBlank()) ? fallback : s;
    }
}
