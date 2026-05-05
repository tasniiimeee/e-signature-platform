package com.example.esignature.model.dto.request;

import lombok.Data;

@Data
public class SubmitToTNNRequest {
    // Optional: can be empty for now
    // Future: add TNN-specific fields like submission type, environment, etc.
    private String environment = "TEST"; // TEST or PRODUCTION
}