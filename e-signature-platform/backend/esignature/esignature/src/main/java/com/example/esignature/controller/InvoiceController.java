package com.example.esignature.controller;

import com.example.esignature.model.dto.request.CreateInvoiceRequest;
import com.example.esignature.model.dto.request.UpdateInvoiceRequest;
import com.example.esignature.model.dto.response.ApiResponse;
import com.example.esignature.model.dto.response.InvoiceResponse;
import com.example.esignature.model.enums.InvoiceStatus;
import com.example.esignature.service.InvoiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/invoices")
@RequiredArgsConstructor
public class InvoiceController {

    private final InvoiceService invoiceService;

    @PostMapping
    public ResponseEntity<InvoiceResponse> createInvoice(@Valid @RequestBody CreateInvoiceRequest request) {
        InvoiceResponse response = invoiceService.createInvoice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<InvoiceResponse>> getAllInvoices() {
        List<InvoiceResponse> invoices = invoiceService.getAllInvoices();
        return ResponseEntity.ok(invoices);
    }

    @GetMapping("/{id}")
    public ResponseEntity<InvoiceResponse> getInvoiceById(@PathVariable UUID id) {
        InvoiceResponse invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(invoice);
    }

    @PutMapping("/{id}")
    public ResponseEntity<InvoiceResponse> updateInvoice(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateInvoiceRequest request
    ) {
        InvoiceResponse response = invoiceService.updateInvoice(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteInvoice(@PathVariable UUID id) {
        invoiceService.deleteInvoice(id);
        return ResponseEntity.ok(ApiResponse.success("Invoice deleted successfully"));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<List<InvoiceResponse>> getInvoicesByStatus(@PathVariable InvoiceStatus status) {
        List<InvoiceResponse> invoices = invoiceService.getInvoicesByStatus(status);
        return ResponseEntity.ok(invoices);
    }
    @GetMapping("/{id}/pdf")
public ResponseEntity<byte[]> downloadPDF(@PathVariable UUID id) {
    byte[] pdfData = invoiceService.generatePDF(id);
    
    return ResponseEntity.ok()
            .header("Content-Type", "application/pdf")
            .header("Content-Disposition", "attachment; filename=invoice.pdf")
            .body(pdfData);
}

@GetMapping("/{id}/pdf/preview")
public ResponseEntity<byte[]> previewPDF(@PathVariable UUID id) {
    byte[] pdfData = invoiceService.generatePDF(id);
    
    return ResponseEntity.ok()
            .header("Content-Type", "application/pdf")
            .header("Content-Disposition", "inline; filename=invoice.pdf")
            .body(pdfData);
}

@GetMapping("/{id}/pdf/url")
public ResponseEntity<ApiResponse> getPDFUrl(@PathVariable UUID id) {
    String pdfUrl = invoiceService.getPDFUrl(id);
    return ResponseEntity.ok(ApiResponse.success("PDF URL retrieved", pdfUrl));
}
}