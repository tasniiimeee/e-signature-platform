package com.example.esignature.controller;

import com.example.esignature.model.dto.request.CreateTemplateRequest;
import com.example.esignature.model.dto.request.UpdateTemplateRequest;
import com.example.esignature.model.dto.response.ApiResponse;
import com.example.esignature.model.dto.response.TemplateResponse;
import com.example.esignature.service.InvoiceTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/templates")
@RequiredArgsConstructor
public class InvoiceTemplateController {

    private final InvoiceTemplateService templateService;

    @PostMapping
    public ResponseEntity<TemplateResponse> createTemplate(@Valid @RequestBody CreateTemplateRequest request) {
        TemplateResponse response = templateService.createTemplate(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TemplateResponse>> getAllTemplates() {
        List<TemplateResponse> templates = templateService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/default")
    public ResponseEntity<TemplateResponse> getDefaultTemplate() {
        TemplateResponse template = templateService.getDefaultTemplate();
        return ResponseEntity.ok(template);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> getTemplateById(@PathVariable UUID id) {
        TemplateResponse template = templateService.getTemplateById(id);
        return ResponseEntity.ok(template);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> updateTemplate(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTemplateRequest request
    ) {
        TemplateResponse response = templateService.updateTemplate(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> deleteTemplate(@PathVariable UUID id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.success("Template deleted successfully"));
    }

    @PostMapping("/{id}/set-default")
    public ResponseEntity<TemplateResponse> setAsDefault(@PathVariable UUID id) {
        TemplateResponse response = templateService.setAsDefault(id);
        return ResponseEntity.ok(response);
    }
}