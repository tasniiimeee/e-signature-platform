package com.example.esignature.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "company_settings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanySettings {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(nullable = false, length = 255)
    private String companyName;
    
    @Column(nullable = false, length = 50)
    private String taxId;
    
    @Column(columnDefinition = "TEXT")
    private String address;
    
    @Column(length = 50)
    private String phone;
    
    @Column(length = 255)
    private String email;
    
    @Column
    private String logoUrl;
    
    // SMTP Settings
    @Column(length = 255)
    private String smtpHost;
    
    @Column
    private Integer smtpPort;
    
    @Column(length = 255)
    private String smtpUsername;
    
    @Column(columnDefinition = "TEXT")
    private String smtpPasswordEncrypted;
    
    // TNN Settings
    @Column(columnDefinition = "TEXT")
    private String tnnApiKeyEncrypted;
    
    @Column(length = 500)
    private String tnnEndpointUrl;
    
    @Column(length = 100)
    private String tnnCompanyId;
    
    // Invoice Settings
    @Column(length = 10)
    private String invoicePrefix = "INV";
    
    @Column
    private Integer invoiceNumberDigits = 5;
    
    @Column(length = 3)
    private String defaultCurrency = "USD";
    
    @Column(precision = 5, scale = 2)
    private BigDecimal defaultVatRate = BigDecimal.valueOf(19.00);
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}