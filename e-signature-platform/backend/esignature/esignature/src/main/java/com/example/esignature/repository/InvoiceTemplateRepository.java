package com.example.esignature.repository;

import com.example.esignature.model.entity.InvoiceTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvoiceTemplateRepository extends JpaRepository<InvoiceTemplate, UUID> {
    
    List<InvoiceTemplate> findByUserId(UUID userId);
    
    Optional<InvoiceTemplate> findByUserIdAndIsDefaultTrue(UUID userId);
}