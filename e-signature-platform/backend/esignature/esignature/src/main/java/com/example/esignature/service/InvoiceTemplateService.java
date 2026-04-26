package com.example.esignature.service;

import com.example.esignature.exception.BadRequestException;
import com.example.esignature.exception.ResourceNotFoundException;
import com.example.esignature.model.dto.request.CreateTemplateRequest;
import com.example.esignature.model.dto.request.UpdateTemplateRequest;
import com.example.esignature.model.dto.response.TemplateResponse;
import com.example.esignature.model.entity.InvoiceTemplate;
import com.example.esignature.model.entity.User;
import com.example.esignature.repository.InvoiceTemplateRepository;
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
public class InvoiceTemplateService {

    private final InvoiceTemplateRepository templateRepository;
    private final UserRepository userRepository;

    @Transactional
    public TemplateResponse createTemplate(CreateTemplateRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // If setting as default, unset other defaults
        if (Boolean.TRUE.equals(request.getIsDefault())) {
            templateRepository.findByUserIdAndIsDefaultTrue(user.getId())
                    .ifPresent(template -> {
                        template.setIsDefault(false);
                        templateRepository.save(template);
                    });
        }

        InvoiceTemplate template = InvoiceTemplate.builder()
                .user(user)
                .name(request.getName())
                .description(request.getDescription())
                .templateData(request.getTemplateData())
                .isDefault(request.getIsDefault())
                .build();

        InvoiceTemplate savedTemplate = templateRepository.save(template);
        
        log.info("Template created: {} by user: {}", savedTemplate.getName(), user.getEmail());

        return convertToResponse(savedTemplate);
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> getAllTemplates() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<InvoiceTemplate> templates = templateRepository.findByUserId(user.getId());
        
        return templates.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TemplateResponse getTemplateById(UUID id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        InvoiceTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        if (!template.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to access this template");
        }

        return convertToResponse(template);
    }

    @Transactional(readOnly = true)
    public TemplateResponse getDefaultTemplate() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        InvoiceTemplate template = templateRepository.findByUserIdAndIsDefaultTrue(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No default template found"));

        return convertToResponse(template);
    }

    @Transactional
    public TemplateResponse updateTemplate(UUID id, UpdateTemplateRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        InvoiceTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        if (!template.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to update this template");
        }

        // If setting as default, unset other defaults
        if (Boolean.TRUE.equals(request.getIsDefault()) && !template.getIsDefault()) {
            templateRepository.findByUserIdAndIsDefaultTrue(user.getId())
                    .ifPresent(existingDefault -> {
                        existingDefault.setIsDefault(false);
                        templateRepository.save(existingDefault);
                    });
        }

        if (request.getName() != null) {
            template.setName(request.getName());
        }
        if (request.getDescription() != null) {
            template.setDescription(request.getDescription());
        }
        if (request.getTemplateData() != null) {
            template.setTemplateData(request.getTemplateData());
        }
        if (request.getIsDefault() != null) {
            template.setIsDefault(request.getIsDefault());
        }

        InvoiceTemplate updatedTemplate = templateRepository.save(template);
        
        log.info("Template updated: {}", updatedTemplate.getName());

        return convertToResponse(updatedTemplate);
    }

    @Transactional
    public void deleteTemplate(UUID id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        InvoiceTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        if (!template.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to delete this template");
        }

        templateRepository.delete(template);
        
        log.info("Template deleted: {}", template.getName());
    }

    @Transactional
    public TemplateResponse setAsDefault(UUID id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        InvoiceTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found"));

        if (!template.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to modify this template");
        }

        // Unset existing default
        templateRepository.findByUserIdAndIsDefaultTrue(user.getId())
                .ifPresent(existingDefault -> {
                    existingDefault.setIsDefault(false);
                    templateRepository.save(existingDefault);
                });

        // Set new default
        template.setIsDefault(true);
        InvoiceTemplate updatedTemplate = templateRepository.save(template);
        
        log.info("Template set as default: {}", updatedTemplate.getName());

        return convertToResponse(updatedTemplate);
    }

    private TemplateResponse convertToResponse(InvoiceTemplate template) {
        return TemplateResponse.builder()
                .id(template.getId())
                .userId(template.getUser().getId())
                .name(template.getName())
                .description(template.getDescription())
                .templateData(template.getTemplateData())
                .isDefault(template.getIsDefault())
                .createdAt(template.getCreatedAt())
                .updatedAt(template.getUpdatedAt())
                .build();
    }
}