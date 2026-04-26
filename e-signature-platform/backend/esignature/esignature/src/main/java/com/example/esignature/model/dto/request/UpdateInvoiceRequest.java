package com.example.esignature.model.dto.request;

import jakarta.validation.Valid;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class UpdateInvoiceRequest {
    
    private UUID clientId;
    
    private LocalDate issueDate;
    
    private LocalDate dueDate;
    
    private String currency;
    
    private String notes;
    
    private String terms;
    
    @Valid
    private List<InvoiceItemRequest> items;
}