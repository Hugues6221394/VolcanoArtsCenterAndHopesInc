package com.volcanoartscenter.platform.shared.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
public class S3StorageService {

    private final String bucketName;
    private final S3Presigner presigner;

    public S3StorageService(
            @Value("${aws.s3.region}") String region,
            @Value("${aws.s3.bucket-name}") String bucketName,
            @Value("${aws.s3.access-key}") String accessKey,
            @Value("${aws.s3.secret-key}") String secretKey) {
        
        this.bucketName = bucketName;

        // Skip presigner initialization if mock keys are present (usually for local dev testing without AWS)
        if ("mock_access_key".equals(accessKey)) {
            log.warn("Mock AWS credentials loaded. Actual S3 presigning will fail if triggered.");
            this.presigner = null;
        } else {
            this.presigner = S3Presigner.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
        }
    }

    /**
     * Generates a short-lived (15 min) HTTP PUT URL.
     * The client application can upload a file directly to this URL, bypassing our JVM memory.
     * 
     * @param directory e.g., "products", "talent", "blogs"
     * @param extension e.g., "jpg", "mp4"
     * @param contentType e.g., "image/jpeg"
     * @return The fully qualified presigned URL
     */
    public PresignedUploadResult generatePresignedUploadUrl(String directory, String extension, String contentType) {
        if (presigner == null) {
            throw new IllegalStateException("AWS S3 is not fully configured on this environment.");
        }

        String objectKey = directory + "/" + UUID.randomUUID() + "." + extension;

        PutObjectRequest objectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(objectKey)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofMinutes(15))
                .putObjectRequest(objectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = presigner.presignPutObject(presignRequest);
        
        // Return both the URL to upload to, and the final objectKey the DB will need natively
        return new PresignedUploadResult(
            presignedRequest.url().toString(),
            objectKey
        );
    }

    public record PresignedUploadResult(String uploadUrl, String targetObjectKey) {}
}
