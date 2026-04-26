package com.example.esignature.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class CreateTemplateRequest {
    
    @NotBlank(message = "Template name is required")
    private String name;
    
    private String description;
    
    private Map<String, Object> templateData;
    
    private Boolean isDefault = false;
}