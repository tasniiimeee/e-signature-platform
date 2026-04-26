package com.example.esignature.service;

import com.example.esignature.config.S3Config;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class S3Service {

    private final S3Client s3Client;
    private final S3Config s3Config;

    public String uploadFile(String key, File file) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(key)
                    .contentType(Files.probeContentType(file.toPath()))
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
            
            String fileUrl = String.format("%s/%s/%s", 
                    s3Config.getEndpoint(), 
                    s3Config.getBucketName(), 
                    key);
            
            log.info("File uploaded to S3: {}", fileUrl);
            return fileUrl;
            
        } catch (Exception e) {
            log.error("Error uploading file to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    public String uploadFile(String key, byte[] data, String contentType) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(key)
                    .contentType(contentType)
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromBytes(data));
            
            String fileUrl = String.format("%s/%s/%s", 
                    s3Config.getEndpoint(), 
                    s3Config.getBucketName(), 
                    key);
            
            log.info("File uploaded to S3: {}", fileUrl);
            return fileUrl;
            
        } catch (Exception e) {
            log.error("Error uploading file to S3: {}", e.getMessage());
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    public InputStream downloadFile(String key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(key)
                    .build();

            return s3Client.getObject(getObjectRequest);
            
        } catch (Exception e) {
            log.error("Error downloading file from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to download file from S3", e);
        }
    }

    public void deleteFile(String key) {
        try {
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(key)
                    .build();

            s3Client.deleteObject(deleteObjectRequest);
            log.info("File deleted from S3: {}", key);
            
        } catch (Exception e) {
            log.error("Error deleting file from S3: {}", e.getMessage());
            throw new RuntimeException("Failed to delete file from S3", e);
        }
    }

    public boolean fileExists(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(s3Config.getBucketName())
                    .key(key)
                    .build();

            s3Client.headObject(headObjectRequest);
            return true;
            
        } catch (NoSuchKeyException e) {
            return false;
        } catch (Exception e) {
            log.error("Error checking file existence in S3: {}", e.getMessage());
            throw new RuntimeException("Failed to check file existence in S3", e);
        }
    }

    public String generateKey(String prefix, String filename) {
        return String.format("%s/%s/%s", prefix, UUID.randomUUID(), filename);
    }
}