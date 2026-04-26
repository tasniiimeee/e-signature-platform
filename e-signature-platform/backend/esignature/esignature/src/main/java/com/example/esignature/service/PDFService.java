package com.example.esignature.service;

import com.example.esignature.exception.BadRequestException;
import com.example.esignature.model.entity.CompanySettings;
import com.example.esignature.model.entity.Invoice;
import com.example.esignature.model.entity.InvoiceItem;
import com.example.esignature.repository.CompanySettingsRepository;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class PDFService {

    private final CompanySettingsRepository companySettingsRepository;
    private final S3Service s3Service;

    public byte[] generateInvoicePDF(Invoice invoice) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Document document = new Document(PageSize.A4, 50, 50, 50, 50);
            PdfWriter.getInstance(document, baos);
            document.open();

            // Get company settings
            CompanySettings settings = companySettingsRepository.findFirstByOrderByCreatedAtAsc()
                    .orElseThrow(() -> new BadRequestException("Company settings not found"));

            // Add header
            addHeader(document, settings);

            // Add spacing
            document.add(new Paragraph("\n"));

            // Add invoice title
            Font titleFont = new Font(Font.FontFamily.HELVETICA, 24, Font.BOLD, new BaseColor(0, 102, 204));
            Paragraph title = new Paragraph("INVOICE", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            document.add(title);

            document.add(new Paragraph("\n"));

            // Add invoice details
            addInvoiceDetails(document, invoice);

            document.add(new Paragraph("\n"));

            // Add items table
            addItemsTable(document, invoice);

            document.add(new Paragraph("\n"));

            // Add totals
            addTotals(document, invoice);

            // Add notes and terms
            if (invoice.getNotes() != null && !invoice.getNotes().isEmpty()) {
                document.add(new Paragraph("\n"));
                Font boldFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
                document.add(new Paragraph("Notes:", boldFont));
                document.add(new Paragraph(invoice.getNotes(), new Font(Font.FontFamily.HELVETICA, 10)));
            }

            if (invoice.getTerms() != null && !invoice.getTerms().isEmpty()) {
                document.add(new Paragraph("\n"));
                Font boldFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);
                document.add(new Paragraph("Payment Terms:", boldFont));
                document.add(new Paragraph(invoice.getTerms(), new Font(Font.FontFamily.HELVETICA, 10)));
            }

            // Add footer
            document.add(new Paragraph("\n\n"));
            Font footerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.ITALIC);
            Paragraph footer = new Paragraph("Thank you for your business!", footerFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            document.add(footer);

            document.close();

            log.info("PDF generated for invoice: {}", invoice.getInvoiceNumber());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Error generating PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF: " + e.getMessage(), e);
        }
    }

    private void addHeader(Document document, CompanySettings settings) throws DocumentException {
        Font companyFont = new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD);
        Paragraph companyName = new Paragraph(settings.getCompanyName(), companyFont);
        document.add(companyName);

        Font detailFont = new Font(Font.FontFamily.HELVETICA, 10);
        if (settings.getAddress() != null) {
            document.add(new Paragraph(settings.getAddress(), detailFont));
        }
        if (settings.getPhone() != null) {
            document.add(new Paragraph("Phone: " + settings.getPhone(), detailFont));
        }
        if (settings.getEmail() != null) {
            document.add(new Paragraph("Email: " + settings.getEmail(), detailFont));
        }
        if (settings.getTaxId() != null) {
            document.add(new Paragraph("Tax ID: " + settings.getTaxId(), detailFont));
        }
    }

    private void addInvoiceDetails(Document document, Invoice invoice) throws DocumentException {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");
        Font labelFont = new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD);
        Font dataFont = new Font(Font.FontFamily.HELVETICA, 10);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setSpacingBefore(10);

        // Left column - Bill To
        PdfPCell billToCell = new PdfPCell();
        billToCell.setBorder(Rectangle.NO_BORDER);
        billToCell.addElement(new Paragraph("BILL TO:", labelFont));
        billToCell.addElement(new Paragraph(invoice.getClient().getName(), dataFont));
        if (invoice.getClient().getAddress() != null) {
            billToCell.addElement(new Paragraph(invoice.getClient().getAddress(), dataFont));
        }
        if (invoice.getClient().getEmail() != null) {
            billToCell.addElement(new Paragraph(invoice.getClient().getEmail(), dataFont));
        }
        if (invoice.getClient().getTaxId() != null) {
            billToCell.addElement(new Paragraph("Tax ID: " + invoice.getClient().getTaxId(), dataFont));
        }

        // Right column - Invoice details
        PdfPCell invoiceDetailsCell = new PdfPCell();
        invoiceDetailsCell.setBorder(Rectangle.NO_BORDER);
        invoiceDetailsCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        invoiceDetailsCell.addElement(new Paragraph("Invoice Number: " + invoice.getInvoiceNumber(), dataFont));
        invoiceDetailsCell.addElement(new Paragraph("Issue Date: " + invoice.getIssueDate().format(dateFormatter), dataFont));
        invoiceDetailsCell.addElement(new Paragraph("Due Date: " + invoice.getDueDate().format(dateFormatter), dataFont));
        invoiceDetailsCell.addElement(new Paragraph("Status: " + invoice.getStatus().toString(), dataFont));

        table.addCell(billToCell);
        table.addCell(invoiceDetailsCell);

        document.add(table);
    }

    private void addItemsTable(Document document, Invoice invoice) throws DocumentException {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3f, 1f, 1.5f, 1.5f, 2f});

        // Header
        BaseColor headerColor = new BaseColor(0, 102, 204);
        Font headerFont = new Font(Font.FontFamily.HELVETICA, 10, Font.BOLD, BaseColor.WHITE);

        table.addCell(createHeaderCell("Description", headerColor, headerFont));
        table.addCell(createHeaderCell("Qty", headerColor, headerFont));
        table.addCell(createHeaderCell("Unit Price", headerColor, headerFont));
        table.addCell(createHeaderCell("Tax Rate", headerColor, headerFont));
        table.addCell(createHeaderCell("Amount", headerColor, headerFont));

        // Items
        Font cellFont = new Font(Font.FontFamily.HELVETICA, 10);
        for (InvoiceItem item : invoice.getItems()) {
            table.addCell(createDataCell(item.getDescription(), cellFont));
            table.addCell(createDataCell(item.getQuantity().toString(), cellFont));
            table.addCell(createDataCell(formatCurrency(item.getUnitPrice(), invoice.getCurrency()), cellFont));
            table.addCell(createDataCell(item.getTaxRate() + "%", cellFont));
            table.addCell(createDataCell(formatCurrency(item.getLineTotal(), invoice.getCurrency()), cellFont));
        }

        document.add(table);
    }

    private void addTotals(Document document, Invoice invoice) throws DocumentException {
        PdfPTable totalsTable = new PdfPTable(2);
        totalsTable.setWidthPercentage(40);
        totalsTable.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Font labelFont = new Font(Font.FontFamily.HELVETICA, 11);
        Font totalFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD);

        // Subtotal
        totalsTable.addCell(createTotalCell("Subtotal:", labelFont));
        totalsTable.addCell(createTotalCell(formatCurrency(invoice.getSubtotal(), invoice.getCurrency()), labelFont));

        // Tax
        totalsTable.addCell(createTotalCell("Tax:", labelFont));
        totalsTable.addCell(createTotalCell(formatCurrency(invoice.getTaxAmount(), invoice.getCurrency()), labelFont));

        // Total
        totalsTable.addCell(createTotalCell("TOTAL:", totalFont));
        totalsTable.addCell(createTotalCell(formatCurrency(invoice.getTotalAmount(), invoice.getCurrency()), totalFont));

        document.add(totalsTable);
    }

    private PdfPCell createHeaderCell(String text, BaseColor bgColor, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(bgColor);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell createDataCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(5);
        return cell;
    }

    private PdfPCell createTotalCell(String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(5);
        return cell;
    }

    private String formatCurrency(BigDecimal amount, String currency) {
        return String.format("%s %.2f", currency, amount);
    }

    public String savePDFToS3(Invoice invoice, byte[] pdfData) {
        String key = s3Service.generateKey("invoices/pdfs", 
                invoice.getInvoiceNumber() + ".pdf");
        
        return s3Service.uploadFile(key, pdfData, "application/pdf");
    }
}