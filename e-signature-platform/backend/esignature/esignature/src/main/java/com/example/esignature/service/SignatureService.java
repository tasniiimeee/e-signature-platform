package com.example.esignature.service;

import com.example.esignature.exception.BadRequestException;
import com.example.esignature.exception.ResourceNotFoundException;
import com.example.esignature.model.dto.request.GenerateKeyRequest;
import com.example.esignature.model.dto.request.SignInvoiceRequest;
import com.example.esignature.model.dto.response.SignatureKeyResponse;
import com.example.esignature.model.entity.Invoice;
import com.example.esignature.model.entity.SignatureKey;
import com.example.esignature.model.entity.User;
import com.example.esignature.model.enums.InvoiceStatus;
import com.example.esignature.repository.InvoiceRepository;
import com.example.esignature.repository.SignatureKeyRepository;
import com.example.esignature.repository.UserRepository;
import com.example.esignature.util.CryptoUtils;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSignatureAppearance;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.security.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SignatureService {

    private final SignatureKeyRepository signatureKeyRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final PDFService pdfService;
    private final S3Service s3Service;

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Transactional
    public SignatureKeyResponse generateKey(GenerateKeyRequest request) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            // Deactivate existing active keys
            signatureKeyRepository.findByUserIdAndIsActiveTrue(user.getId())
                    .ifPresent(key -> {
                        key.setIsActive(false);
                        signatureKeyRepository.save(key);
                    });

            // Generate RSA key pair
            KeyPair keyPair = CryptoUtils.generateRSAKeyPair(request.getKeySize());

            // Encrypt private key with passphrase
            String encryptedPrivateKey = CryptoUtils.encryptPrivateKey(
                    keyPair.getPrivate(), 
                    request.getPassphrase()
            );

            // Store public key
            String publicKeyStr = CryptoUtils.publicKeyToString(keyPair.getPublic());

            // Create signature key entity
            SignatureKey signatureKey = SignatureKey.builder()
                    .user(user)
                    .keyName(request.getKeyName())
                    .publicKey(publicKeyStr)
                    .privateKeyEncrypted(encryptedPrivateKey)
                    .algorithm("RSA-" + request.getKeySize())
                    .isActive(true)
                    .expiresAt(LocalDateTime.now().plusYears(1))
                    .build();

            SignatureKey savedKey = signatureKeyRepository.save(signatureKey);

            log.info("Signature key generated: {} for user: {}", savedKey.getKeyName(), user.getEmail());

            return convertToResponse(savedKey);

        } catch (Exception e) {
            log.error("Error generating signature key: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate signature key: " + e.getMessage());
        }
    }

    @Transactional
    public void signInvoice(UUID invoiceId, SignInvoiceRequest request) {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));

            Invoice invoice = invoiceRepository.findById(invoiceId)
                    .orElseThrow(() -> new ResourceNotFoundException("Invoice not found"));

            if (!invoice.getUser().getId().equals(user.getId())) {
                throw new BadRequestException("You don't have permission to sign this invoice");
            }

            if (invoice.getStatus() != InvoiceStatus.DRAFT) {
                throw new BadRequestException("Only draft invoices can be signed");
            }

            SignatureKey signatureKey = signatureKeyRepository.findById(request.getSignatureKeyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Signature key not found"));

            if (!signatureKey.getUser().getId().equals(user.getId())) {
                throw new BadRequestException("Signature key does not belong to you");
            }

            if (signatureKey.getRevokedAt() != null) {
                throw new BadRequestException("Signature key has been revoked");
            }

            // Decrypt private key
            byte[] privateKeyBytes = CryptoUtils.decryptPrivateKey(
                    signatureKey.getPrivateKeyEncrypted(), 
                    request.getPassphrase()
            );

            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            // Generate unsigned PDF
            byte[] unsignedPdf = pdfService.generateInvoicePDF(invoice);

            // Sign PDF
            byte[] signedPdf = signPDF(unsignedPdf, privateKey);

            // Calculate hash
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(signedPdf);
            String signatureHash = Base64.getEncoder().encodeToString(hash);

            // Save signed PDF to S3
            String pdfUrl = pdfService.savePDFToS3(invoice, signedPdf);

            // Update invoice
            invoice.setStatus(InvoiceStatus.SIGNED);
            invoice.setPdfUrl(pdfUrl);
            invoice.setSignatureHash(signatureHash);
            invoice.setSignedAt(LocalDateTime.now());
            invoice.setSignedBy(user);

            invoiceRepository.save(invoice);

            log.info("Invoice signed: {} by user: {}", invoice.getInvoiceNumber(), user.getEmail());

        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error signing invoice: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to sign invoice: " + e.getMessage());
        }
    }

private byte[] signPDF(byte[] pdfData, PrivateKey privateKey) throws Exception {
    PdfReader reader = new PdfReader(new ByteArrayInputStream(pdfData));
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PdfStamper stamper = PdfStamper.createSignature(reader, baos, '\0');

    PdfSignatureAppearance appearance = stamper.getSignatureAppearance();
    appearance.setReason("Invoice Signature");
    appearance.setLocation("E-Signature Platform");

    ExternalDigest digest = new BouncyCastleDigest();
    ExternalSignature signature = new PrivateKeySignature(privateKey, "SHA-256", "BC");

   
    MakeSignature.signDetached(
        appearance, 
        digest, 
        signature, 
        null,  
        null, 
        null,  
        null, 
        0,     
        MakeSignature.CryptoStandard.CMS
    );

    stamper.close();
    reader.close();

    return baos.toByteArray();
}
    @Transactional(readOnly = true)
    public List<SignatureKeyResponse> getAllKeys() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<SignatureKey> keys = signatureKeyRepository.findByUserId(user.getId());

        return keys.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SignatureKeyResponse getActiveKey() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SignatureKey key = signatureKeyRepository.findByUserIdAndIsActiveTrue(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No active signature key found"));

        return convertToResponse(key);
    }

    @Transactional
    public void revokeKey(UUID keyId) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SignatureKey key = signatureKeyRepository.findById(keyId)
                .orElseThrow(() -> new ResourceNotFoundException("Signature key not found"));

        if (!key.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Signature key does not belong to you");
        }

        key.setIsActive(false);
        key.setRevokedAt(LocalDateTime.now());
        signatureKeyRepository.save(key);

        log.info("Signature key revoked: {}", key.getKeyName());
    }

    private SignatureKeyResponse convertToResponse(SignatureKey key) {
        return SignatureKeyResponse.builder()
                .id(key.getId())
                .userId(key.getUser().getId())
                .keyName(key.getKeyName())
                .publicKey(key.getPublicKey())
                .algorithm(key.getAlgorithm())
                .isActive(key.getIsActive())
                .expiresAt(key.getExpiresAt())
                .createdAt(key.getCreatedAt())
                .revokedAt(key.getRevokedAt())
                .build();
    }
}