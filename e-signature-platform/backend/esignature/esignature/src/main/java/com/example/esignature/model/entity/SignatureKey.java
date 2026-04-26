package com.example.esignature.model.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "signature_keys")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignatureKey {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false, length = 100)
    private String keyName;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String publicKey;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String privateKeyEncrypted;
    
    @Column(nullable = false, length = 50)
    private String algorithm;
    
    @Column(columnDefinition = "TEXT")
    private String certificate;
    
    @Column(nullable = false)
    private Boolean isActive = true;
    
    @Column
    private LocalDateTime expiresAt;
    
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime revokedAt;
}