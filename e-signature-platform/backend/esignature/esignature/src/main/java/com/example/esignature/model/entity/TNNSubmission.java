package com.example.esignature.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tnn_submissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TNNSubmission {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String submissionPayload;
    
    @Column(columnDefinition = "TEXT")
    private String responsePayload;
    
    @Column(nullable = false, length = 30)
    private String status;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column
    private String tnnSubmissionId;
    
    @Column
    private Integer retryCount = 0;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime submittedAt;
    
    @Column
    private LocalDateTime processedAt;
}