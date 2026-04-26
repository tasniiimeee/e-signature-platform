package com.example.esignature.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class GenerateKeyRequest {
    
    @NotBlank(message = "Key name is required")
    @Size(min = 3, max = 100, message = "Key name must be between 3 and 100 characters")
    private String keyName;
    
    @NotBlank(message = "Passphrase is required")
    @Size(min = 8, message = "Passphrase must be at least 8 characters")
    private String passphrase;
    
    private Integer keySize = 2048; // RSA key size
}