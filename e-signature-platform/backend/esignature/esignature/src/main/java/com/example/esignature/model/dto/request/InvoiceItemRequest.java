package com.example.esignature.model.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class InvoiceItemRequest {
    
    @NotBlank(message = "Description is required")
    private String description;
    
    @NotNull(message = "Quantity is required")
    @DecimalMin(value = "0.01", message = "Quantity must be greater than 0")
    private BigDecimal quantity;
    
    @NotNull(message = "Unit price is required")
    @DecimalMin(value = "0.00", message = "Unit price must be greater than or equal to 0")
    private BigDecimal unitPrice;
    
    @NotNull(message = "Tax rate is required")
    @DecimalMin(value = "0.00", message = "Tax rate must be greater than or equal to 0")
    private BigDecimal taxRate;
    
    private Integer sortOrder;
}