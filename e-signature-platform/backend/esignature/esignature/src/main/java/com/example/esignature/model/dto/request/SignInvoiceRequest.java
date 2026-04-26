package com.example.esignature.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class SignInvoiceRequest {
    
    @NotNull(message = "Signature key ID is required")
    private UUID signatureKeyId;
    
    @NotBlank(message = "Passphrase is required")
    private String passphrase;
}