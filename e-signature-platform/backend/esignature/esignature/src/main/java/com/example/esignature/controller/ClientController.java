package com.example.esignature.controller;

import com.example.esignature.model.dto.request.CreateClientRequest;
import com.example.esignature.model.dto.request.UpdateClientRequest;
import com.example.esignature.model.dto.response.ApiResponse;
import com.example.esignature.model.dto.response.ClientResponse;
import com.example.esignature.service.ClientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/clients")
@RequiredArgsConstructor
public class ClientController {

    private final ClientService clientService;

    @PostMapping
    public ResponseEntity<ClientResponse> createClient(@Valid @RequestBody CreateClientRequest request) {
        ClientResponse response = clientService.createClient(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ClientResponse>> getAllClients() {
        List<ClientResponse> clients = clientService.getAllClients();
        return ResponseEntity.ok(clients);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClientResponse> getClientById(@PathVariable UUID id) {
        ClientResponse client = clientService.getClientById(id);
        return ResponseEntity.ok(client);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ClientResponse> updateClient(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClientRequest request
    ) {
        ClientResponse response = clientService.updateClient(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteClient(@PathVariable UUID id) {
        clientService.deleteClient(id);
        return ResponseEntity.ok(ApiResponse.success("Client deleted successfully"));
    }

    @GetMapping("/search")
    public ResponseEntity<List<ClientResponse>> searchClients(@RequestParam String query) {
        List<ClientResponse> clients = clientService.searchClients(query);
        return ResponseEntity.ok(clients);
    }
}