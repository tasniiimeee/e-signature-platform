package com.example.esignature.repository;

import com.example.esignature.model.entity.SignatureKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SignatureKeyRepository extends JpaRepository<SignatureKey, UUID> {
    
    List<SignatureKey> findByUserId(UUID userId);
    
    Optional<SignatureKey> findByUserIdAndIsActiveTrue(UUID userId);
    
    List<SignatureKey> findByUserIdAndRevokedAtIsNull(UUID userId);
}