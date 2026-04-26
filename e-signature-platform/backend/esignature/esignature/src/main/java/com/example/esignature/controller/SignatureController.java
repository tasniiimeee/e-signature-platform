package com.example.esignature.controller;

import com.example.esignature.model.dto.request.GenerateKeyRequest;
import com.example.esignature.model.dto.request.SignInvoiceRequest;
import com.example.esignature.model.dto.response.ApiResponse;
import com.example.esignature.model.dto.response.SignatureKeyResponse;
import com.example.esignature.service.SignatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/signature")
@RequiredArgsConstructor
public class SignatureController {

    private final SignatureService signatureService;

    @PostMapping("/keys/generate")
    public ResponseEntity<SignatureKeyResponse> generateKey(@Valid @RequestBody GenerateKeyRequest request) {
        SignatureKeyResponse response = signatureService.generateKey(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/keys")
    public ResponseEntity<List<SignatureKeyResponse>> getAllKeys() {
        List<SignatureKeyResponse> keys = signatureService.getAllKeys();
        return ResponseEntity.ok(keys);
    }

    @GetMapping("/keys/active")
    public ResponseEntity<SignatureKeyResponse> getActiveKey() {
        SignatureKeyResponse key = signatureService.getActiveKey();
        return ResponseEntity.ok(key);
    }

    @PostMapping("/keys/{id}/revoke")
    public ResponseEntity<ApiResponse> revokeKey(@PathVariable UUID id) {
        signatureService.revokeKey(id);
        return ResponseEntity.ok(ApiResponse.success("Signature key revoked successfully"));
    }

    @PostMapping("/invoices/{invoiceId}/sign")
    public ResponseEntity<ApiResponse> signInvoice(
            @PathVariable UUID invoiceId,
            @Valid @RequestBody SignInvoiceRequest request
    ) {
        signatureService.signInvoice(invoiceId, request);
        return ResponseEntity.ok(ApiResponse.success("Invoice signed successfully"));
    }
}