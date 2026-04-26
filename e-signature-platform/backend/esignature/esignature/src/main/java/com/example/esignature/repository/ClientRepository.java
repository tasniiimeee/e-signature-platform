package com.example.esignature.repository;

import com.example.esignature.model.entity.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ClientRepository extends JpaRepository<Client, UUID> {
    
    List<Client> findByUserId(UUID userId);
    
    List<Client> findByUserIdAndNameContainingIgnoreCase(UUID userId, String name);
}