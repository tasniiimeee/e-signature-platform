package com.example.esignature.model.dto.response;

import com.example.esignature.model.enums.InvoiceStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InvoiceResponse {
    private UUID id;
    private UUID userId;
    private ClientResponse client;
    private String invoiceNumber;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private InvoiceStatus status;
    private BigDecimal subtotal;
    private BigDecimal taxAmount;
    private BigDecimal totalAmount;
    private String currency;
    private String notes;
    private String terms;
    private String pdfUrl;
    private String xmlUrl;
    private String signatureHash;
    private LocalDateTime signedAt;
    private UserResponse signedBy;
    private String tnnSubmissionId;
    private String tnnSubmissionStatus;
    private LocalDateTime tnnSubmittedAt;
    private List<InvoiceItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}