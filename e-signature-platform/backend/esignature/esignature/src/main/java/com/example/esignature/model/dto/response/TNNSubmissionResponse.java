package com.example.esignature.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TNNSubmissionResponse {
    private UUID id;
    private UUID invoiceId;
    private String invoiceNumber;
    private String status;
    private String tnnSubmissionId;
    private String errorMessage;
    private Integer retryCount;
    private LocalDateTime submittedAt;
    private LocalDateTime processedAt;
}