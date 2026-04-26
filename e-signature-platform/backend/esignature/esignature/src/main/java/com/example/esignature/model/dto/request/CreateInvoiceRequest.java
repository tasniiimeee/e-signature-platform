package com.example.esignature.model.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Data
public class CreateInvoiceRequest {
    
    @NotNull(message = "Client ID is required")
    private UUID clientId;
    
    @NotNull(message = "Issue date is required")
    private LocalDate issueDate;
    
    private LocalDate dueDate;
    
    private String currency = "USD";
    
    private String notes;
    
    private String terms;
    
    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<InvoiceItemRequest> items;
}