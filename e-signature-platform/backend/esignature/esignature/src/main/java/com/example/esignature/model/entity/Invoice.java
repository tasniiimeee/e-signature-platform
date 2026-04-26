package com.example.esignature.model.entity;

import com.example.esignature.model.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    
    @Column(unique = true, nullable = false, length = 50)
    private String invoiceNumber;
    
    @Column(nullable = false)
    private LocalDate issueDate;
    
    @Column
    private LocalDate dueDate;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InvoiceStatus status;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal taxAmount;
    
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;
    
    @Column(length = 3)
    private String currency = "USD";
    
    @Column(columnDefinition = "TEXT")
    private String notes;
    
    @Column(columnDefinition = "TEXT")
    private String terms;
    
    @Column
    private String pdfUrl;
    
    @Column
    private String xmlUrl;
    
    @Column(length = 256)
    private String signatureHash;
    
    @Column
    private LocalDateTime signedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signed_by")
    private User signedBy;
    
    @Column(length = 100)
    private String tnnSubmissionId;
    
    @Column(length = 30)
    private String tnnSubmissionStatus;
    
    @Column
    private LocalDateTime tnnSubmittedAt;
    
    @OneToMany(mappedBy = "invoice", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<InvoiceItem> items = new ArrayList<>();
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    // Helper methods
    public void addItem(InvoiceItem item) {
        items.add(item);
        item.setInvoice(this);
    }
    
    public void removeItem(InvoiceItem item) {
        items.remove(item);
        item.setInvoice(null);
    }
}