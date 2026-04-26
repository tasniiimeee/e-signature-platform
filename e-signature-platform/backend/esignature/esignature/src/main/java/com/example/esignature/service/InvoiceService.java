package com.example.esignature.service;

import com.example.esignature.exception.BadRequestException;
import com.example.esignature.exception.ResourceNotFoundException;
import com.example.esignature.model.dto.request.CreateInvoiceRequest;
import com.example.esignature.model.dto.request.InvoiceItemRequest;
import com.example.esignature.model.dto.request.UpdateInvoiceRequest;
import com.example.esignature.model.dto.response.ClientResponse;
import com.example.esignature.model.dto.response.InvoiceItemResponse;
import com.example.esignature.model.dto.response.InvoiceResponse;
import com.example.esignature.model.dto.response.UserResponse;
import com.example.esignature.model.entity.Client;
import com.example.esignature.model.entity.Invoice;
import com.example.esignature.model.entity.InvoiceItem;
import com.example.esignature.model.entity.User;
import com.example.esignature.model.enums.InvoiceStatus;
import com.example.esignature.repository.ClientRepository;
import com.example.esignature.repository.InvoiceRepository;
import com.example.esignature.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final PDFService pdfService;

    @Transactional
    public InvoiceResponse createInvoice(CreateInvoiceRequest request) {
        // Get current user
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        // Get client and verify ownership
        Client client = clientRepository.findById(request.getClientId())
                .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        if (!client.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Client does not belong to you");
        }

        // Generate invoice number
        String invoiceNumber = generateInvoiceNumber();

        // Create invoice
        Invoice invoice = Invoice.builder()
                .user(user)
                .client(client)
                .invoiceNumber(invoiceNumber)
                .issueDate(request.getIssueDate())
                .dueDate(request.getDueDate() != null ? request.getDueDate() : request.getIssueDate().plusDays(30))
                .status(InvoiceStatus.DRAFT)
                .currency(request.getCurrency())
                .notes(request.getNotes())
                .terms(request.getTerms())
                .items(new ArrayList<>())
                .subtotal(BigDecimal.ZERO)
                .taxAmount(BigDecimal.ZERO)
                .totalAmount(BigDecimal.ZERO)
                .build();

        // Add items
        int sortOrder = 1;
        for (InvoiceItemRequest itemRequest : request.getItems()) {
            InvoiceItem item = InvoiceItem.builder()
                    .invoice(invoice)
                    .description(itemRequest.getDescription())
                    .quantity(itemRequest.getQuantity())
                    .unitPrice(itemRequest.getUnitPrice())
                    .taxRate(itemRequest.getTaxRate())
                    .sortOrder(itemRequest.getSortOrder() != null ? itemRequest.getSortOrder() : sortOrder++)
                    .build();
            
            item.calculateLineTotal();
            invoice.addItem(item);
        }

        // Calculate totals
        calculateTotals(invoice);

        Invoice savedInvoice = invoiceRepository.save(invoice);
        
        log.info("Invoice created: {} by user: {}", savedInvoice.getInvoiceNumber(), user.getEmail());

        return convertToResponse(savedInvoice);
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getAllInvoices() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Invoice> invoices = invoiceRepository.findByUserId(user.getId());
        
        return invoices.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public InvoiceResponse getInvoiceById(UUID id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (!invoice.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to access this invoice");
        }

        return convertToResponse(invoice);
    }

    @Transactional
    public InvoiceResponse updateInvoice(UUID id, UpdateInvoiceRequest request) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (!invoice.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to update this invoice");
        }

        // Can only update draft invoices
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BadRequestException("Only draft invoices can be updated");
        }

        // Update fields
        if (request.getClientId() != null) {
            Client client = clientRepository.findById(request.getClientId())
                    .orElseThrow(() -> new ResourceNotFoundException("Client not found"));
            
            if (!client.getUser().getId().equals(user.getId())) {
                throw new BadRequestException("Client does not belong to you");
            }
            
            invoice.setClient(client);
        }

        if (request.getIssueDate() != null) {
            invoice.setIssueDate(request.getIssueDate());
        }

        if (request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate());
        }

        if (request.getCurrency() != null) {
            invoice.setCurrency(request.getCurrency());
        }

        if (request.getNotes() != null) {
            invoice.setNotes(request.getNotes());
        }

        if (request.getTerms() != null) {
            invoice.setTerms(request.getTerms());
        }

        // Update items if provided
        if (request.getItems() != null) {
            // Remove old items
            invoice.getItems().clear();

            // Add new items
            int sortOrder = 1;
            for (InvoiceItemRequest itemRequest : request.getItems()) {
                InvoiceItem item = InvoiceItem.builder()
                        .invoice(invoice)
                        .description(itemRequest.getDescription())
                        .quantity(itemRequest.getQuantity())
                        .unitPrice(itemRequest.getUnitPrice())
                        .taxRate(itemRequest.getTaxRate())
                        .sortOrder(itemRequest.getSortOrder() != null ? itemRequest.getSortOrder() : sortOrder++)
                        .build();
                
                item.calculateLineTotal();
                invoice.addItem(item);
            }

            // Recalculate totals
            calculateTotals(invoice);
        }

        Invoice updatedInvoice = invoiceRepository.save(invoice);
        
        log.info("Invoice updated: {}", updatedInvoice.getInvoiceNumber());

        return convertToResponse(updatedInvoice);
    }

    @Transactional
    public void deleteInvoice(UUID id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

        if (!invoice.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("You don't have permission to delete this invoice");
        }

        // Can only delete draft invoices
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new BadRequestException("Only draft invoices can be deleted");
        }

        invoiceRepository.delete(invoice);
        
        log.info("Invoice deleted: {}", invoice.getInvoiceNumber());
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> getInvoicesByStatus(InvoiceStatus status) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Invoice> invoices = invoiceRepository.findByUserIdAndStatus(user.getId(), status);
        
        return invoices.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private void calculateTotals(Invoice invoice) {
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;

        for (InvoiceItem item : invoice.getItems()) {
            subtotal = subtotal.add(item.getLineTotal());
            
            BigDecimal itemTax = item.getLineTotal()
                    .multiply(item.getTaxRate())
                    .divide(BigDecimal.valueOf(100), 2, BigDecimal.ROUND_HALF_UP);
            
            taxAmount = taxAmount.add(itemTax);
        }

        invoice.setSubtotal(subtotal);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalAmount(subtotal.add(taxAmount));
    }

    private String generateInvoiceNumber() {
        // Format: INV-YYYY-00001
        int year = LocalDate.now().getYear();
        String prefix = "INV-" + year + "-";
        
        // Find last invoice number for this year
        String lastNumber = prefix + "00000";
        
        // This is simplified - in production, use a sequence or better logic
        long count = invoiceRepository.count() + 1;
        
        return String.format("INV-%d-%05d", year, count);
    }

    private InvoiceResponse convertToResponse(Invoice invoice) {
        return InvoiceResponse.builder()
                .id(invoice.getId())
                .userId(invoice.getUser().getId())
                .client(convertClientToResponse(invoice.getClient()))
                .invoiceNumber(invoice.getInvoiceNumber())
                .issueDate(invoice.getIssueDate())
                .dueDate(invoice.getDueDate())
                .status(invoice.getStatus())
                .subtotal(invoice.getSubtotal())
                .taxAmount(invoice.getTaxAmount())
                .totalAmount(invoice.getTotalAmount())
                .currency(invoice.getCurrency())
                .notes(invoice.getNotes())
                .terms(invoice.getTerms())
                .pdfUrl(invoice.getPdfUrl())
                .xmlUrl(invoice.getXmlUrl())
                .signatureHash(invoice.getSignatureHash())
                .signedAt(invoice.getSignedAt())
                .signedBy(invoice.getSignedBy() != null ? convertUserToResponse(invoice.getSignedBy()) : null)
                .tnnSubmissionId(invoice.getTnnSubmissionId())
                .tnnSubmissionStatus(invoice.getTnnSubmissionStatus())
                .tnnSubmittedAt(invoice.getTnnSubmittedAt())
                .items(invoice.getItems().stream()
                        .map(this::convertItemToResponse)
                        .collect(Collectors.toList()))
                .createdAt(invoice.getCreatedAt())
                .updatedAt(invoice.getUpdatedAt())
                .build();
    }

    private ClientResponse convertClientToResponse(Client client) {
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

    private UserResponse convertUserToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .isActive(user.getIsActive())
                .isEmailVerified(user.getIsEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }

    private InvoiceItemResponse convertItemToResponse(InvoiceItem item) {
        return InvoiceItemResponse.builder()
                .id(item.getId())
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .taxRate(item.getTaxRate())
                .lineTotal(item.getLineTotal())
                .sortOrder(item.getSortOrder())
                .createdAt(item.getCreatedAt())
                .build();
    }
    @Transactional
public byte[] generatePDF(UUID invoiceId) {
    String email = SecurityContextHolder.getContext().getAuthentication().getName();
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

    if (!invoice.getUser().getId().equals(user.getId())) {
        throw new BadRequestException("You don't have permission to access this invoice");
    }

    // Generate PDF
    byte[] pdfData = pdfService.generateInvoicePDF(invoice);

    // Save to S3 if not already saved
    if (invoice.getPdfUrl() == null) {
        String pdfUrl = pdfService.savePDFToS3(invoice, pdfData);
        invoice.setPdfUrl(pdfUrl);
        invoiceRepository.save(invoice);
    }

    return pdfData;
}

@Transactional(readOnly = true)
public String getPDFUrl(UUID invoiceId) {
    String email = SecurityContextHolder.getContext().getAuthentication().getName();
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

    Invoice invoice = invoiceRepository.findById(invoiceId)
            .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

    if (!invoice.getUser().getId().equals(user.getId())) {
        throw new BadRequestException("You don't have permission to access this invoice");
    }

    if (invoice.getPdfUrl() == null) {
        // Generate and save PDF
        byte[] pdfData = pdfService.generateInvoicePDF(invoice);
        String pdfUrl = pdfService.savePDFToS3(invoice, pdfData);
        invoice.setPdfUrl(pdfUrl);
        invoiceRepository.save(invoice);
        return pdfUrl;
    }

    return invoice.getPdfUrl();
}
}