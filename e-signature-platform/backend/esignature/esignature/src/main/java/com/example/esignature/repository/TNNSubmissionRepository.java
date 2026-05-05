package com.example.esignature.repository;

import com.example.esignature.model.entity.TNNSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TNNSubmissionRepository extends JpaRepository<TNNSubmission, UUID> {
    
    List<TNNSubmission> findByInvoiceId(UUID invoiceId);
    
    Optional<TNNSubmission> findByTnnSubmissionId(String tnnSubmissionId);
    
    List<TNNSubmission> findByStatus(String status);
}