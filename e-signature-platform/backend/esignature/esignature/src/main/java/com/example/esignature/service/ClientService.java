package com.example.esignature.service;

import com.example.esignature.exception.BadRequestException;
import com.example.esignature.exception.ResourceNotFoundException;
import com.example.esignature.model.dto.request.CreateClientRequest;
import com.example.esignature.model.dto.request.UpdateClientRequest;
import com.example.esignature.model.dto.response.ClientResponse;
import com.example.esignature.model.entity.Client;
import com.example.esignature.model.entity.User;
import com.example.esignature.repository.ClientRepository;
import com.example.esignature.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientService {

    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    @Transactional
    public ClientResponse createClient(CreateClientRequest request) {
        // Get current authenticated user
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Create client
        Client client = Client.builder()
                .user(user)
                .name(request.getName())
                .taxId(request.getTaxId())
                .address(request.getAddress())
                .email(request.getEmail())
                .phone(request.getPhone())
                .build();

        Client savedClient = clientRepository.save(client);
        
        log.info("Client created: {} by user: {}", savedClient.getId(), user.getEmail());

        return convertToResponse(savedClient);
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> getAllClients() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Client> clients = clientRepository.findByUserId(user.getId());
        
        return clients.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ClientResponse getClientById(UUID id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        // Verify client belongs to current user
        if (!client.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to access this client");
        }

        return convertToResponse(client);
    }

    @Transactional
    public ClientResponse updateClient(UUID id, UpdateClientRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        // Verify client belongs to current user
        if (!client.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to update this client");
        }

        // Update fields
        if (request.getName() != null) {
            client.setName(request.getName());
        }
        if (request.getTaxId() != null) {
            client.setTaxId(request.getTaxId());
        }
        if (request.getAddress() != null) {
            client.setAddress(request.getAddress());
        }
        if (request.getEmail() != null) {
            client.setEmail(request.getEmail());
        }
        if (request.getPhone() != null) {
            client.setPhone(request.getPhone());
        }

        Client updatedClient = clientRepository.save(client);
        
        log.info("Client updated: {}", updatedClient.getId());

        return convertToResponse(updatedClient);
    }

    @Transactional
    public void deleteClient(UUID id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Client client = clientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        // Verify client belongs to current user
        if (!client.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to delete this client");
        }

        clientRepository.delete(client);
        
        log.info("Client deleted: {}", id);
    }

    @Transactional(readOnly = true)
    public List<ClientResponse> searchClients(String query) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Client> clients = clientRepository.findByUserIdAndNameContainingIgnoreCase(user.getId(), query);
        
        return clients.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private ClientResponse convertToResponse(Client client) {
        return ClientResponse.builder()
                .id(client.getId())
                .userId(client.getUser().getId())
                .name(client.getName())
                .taxId(client.getTaxId())
                .address(client.getAddress())
                .email(client.getEmail())
                .phone(client.getPhone())
                .createdAt(client.getCreatedAt())
                .updatedAt(client.getUpdatedAt())
                .build();
    }
}