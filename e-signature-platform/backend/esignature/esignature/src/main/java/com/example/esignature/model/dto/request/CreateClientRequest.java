package com.example.esignature.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateClientRequest {
    
    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 255, message = "Name must be between 2 and 255 characters")
    private String name;
    
    @Size(max = 50, message = "Tax ID must not exceed 50 characters")
    private String taxId;
    
    private String address;
    
    @Email(message = "Email must be valid")
    private String email;
    
    @Size(max = 50, message = "Phone must not exceed 50 characters")
    private String phone;
}