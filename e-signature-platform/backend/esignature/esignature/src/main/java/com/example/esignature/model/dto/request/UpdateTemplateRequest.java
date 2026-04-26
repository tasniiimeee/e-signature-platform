package com.example.esignature.model.dto.request;

import lombok.Data;

import java.util.Map;

@Data
public class UpdateTemplateRequest {
    
    private String name;
    
    private String description;
    
    private Map<String, Object> templateData;
    
    private Boolean isDefault;
}