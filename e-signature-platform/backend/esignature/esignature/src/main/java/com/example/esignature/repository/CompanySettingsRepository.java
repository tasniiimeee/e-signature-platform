package com.example.esignature.repository;

import com.example.esignature.model.entity.CompanySettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CompanySettingsRepository extends JpaRepository<CompanySettings, UUID> {
    
    Optional<CompanySettings> findFirstByOrderByCreatedAtAsc();
}