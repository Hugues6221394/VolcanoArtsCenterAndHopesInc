package com.volcanoartscenter.platform.shared.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Derives 3 JPEG variants (thumb 200w, medium 800w, large 1600w) from a base
 * image already uploaded to S3 via the presigned-PUT flow. Each variant is
 * written alongside the original at a derivative key:
 * <pre>
 *   products/abc.jpg              ← original
 *   products/abc_thumb.jpg        ← 200w
 *   products/abc_medium.jpg       ← 800w
 *   products/abc_large.jpg        ← 1600w
 * </pre>
 *
 * <p>Aspect ratio is preserved; if the source is smaller than a target width
 * the original dimensions are kept (no upscaling).
 *
 * <p>WebP and blurhash placeholders are intentionally not yet generated —
 * they require additional dependencies (webp-imageio, blurhash) that are best
 * vetted alongside live S3 access. They're tracked as a Phase 7 follow-up.
 */
@Service
@Slf4j
public class ImagePipelineService {

    public static final int THUMB_WIDTH = 200;
    public static final int MEDIUM_WIDTH = 800;
    public static final int LARGE_WIDTH = 1600;

    private final String bucketName;
    private final S3Client s3Client;

    public ImagePipelineService(
            @Value("${aws.s3.region:us-east-1}") String region,
            @Value("${aws.s3.bucket-name:}") String bucketName,
            @Value("${aws.s3.access-key:mock_access_key}") String accessKey,
            @Value("${aws.s3.secret-key:mock_secret_key}") String secretKey) {
        this.bucketName = bucketName;
        if ("mock_access_key".equals(accessKey) || bucketName == null || bucketName.isBlank()) {
            log.warn("ImagePipelineService running without S3 credentials — derivativeKeys() returns predictions only.");
            this.s3Client = null;
        } else {
            this.s3Client = S3Client.builder()
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .build();
        }
    }

    /**
     * Maps an original key to its predicted derivative keys without touching S3.
     * Useful for setting the resized URLs on a Product record before the pipeline runs.
     */
    public DerivativeKeys derivativeKeys(String originalKey) {
        return new DerivativeKeys(
                originalKey,
                derive(originalKey, "thumb"),
                derive(originalKey, "medium"),
                derive(originalKey, "large"));
    }

    /**
     * Reads the original from S3, generates 3 JPEG variants, writes them back
     * with predictable keys, and returns the result.
     */
    public DerivativeKeys generateVariants(String originalKey) {
        if (s3Client == null) {
            throw new IllegalStateException("AWS S3 is not fully configured on this environment.");
        }
        byte[] original = downloadOriginal(originalKey);
        BufferedImage src;
        try {
            src = ImageIO.read(new ByteArrayInputStream(original));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to decode source image " + originalKey, ex);
        }
        if (src == null) {
            throw new IllegalStateException("Source object " + originalKey + " is not a decodable image.");
        }

        Map<String, String> uploaded = new LinkedHashMap<>();
        uploaded.put("thumb", uploadVariant(originalKey, "thumb", resize(src, THUMB_WIDTH)));
        uploaded.put("medium", uploadVariant(originalKey, "medium", resize(src, MEDIUM_WIDTH)));
        uploaded.put("large", uploadVariant(originalKey, "large", resize(src, LARGE_WIDTH)));

        log.info("Image pipeline produced 3 variants for {}", originalKey);
        return new DerivativeKeys(originalKey, uploaded.get("thumb"), uploaded.get("medium"), uploaded.get("large"));
    }

    private byte[] downloadOriginal(String key) {
        GetObjectRequest req = GetObjectRequest.builder().bucket(bucketName).key(key).build();
        try (ResponseInputStream<GetObjectResponse> in = s3Client.getObject(req);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to fetch S3 object " + key, ex);
        }
    }

    private byte[] resize(BufferedImage src, int targetWidth) {
        int srcW = src.getWidth();
        int srcH = src.getHeight();
        int width = Math.min(srcW, targetWidth);
        int height = (int) Math.round((double) srcH * width / srcW);
        BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Image scaled = src.getScaledInstance(width, height, Image.SCALE_SMOOTH);
            g.drawImage(scaled, 0, 0, null);
        } finally {
            g.dispose();
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            if (!ImageIO.write(out, "jpeg", baos)) {
                throw new IllegalStateException("No JPEG ImageIO writer available");
            }
            return baos.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encode resized image", ex);
        }
    }

    private String uploadVariant(String originalKey, String suffix, byte[] bytes) {
        String key = derive(originalKey, suffix);
        PutObjectRequest req = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(key)
                .contentType("image/jpeg")
                .contentLength((long) bytes.length)
                .build();
        s3Client.putObject(req, RequestBody.fromBytes(bytes));
        return key;
    }

    private String derive(String key, String suffix) {
        if (key == null || key.isEmpty()) return suffix;
        int dot = key.lastIndexOf('.');
        if (dot <= 0) return key + "_" + suffix + ".jpg";
        return key.substring(0, dot) + "_" + suffix + ".jpg";
    }

    public record DerivativeKeys(String original, String thumb, String medium, String large) {}
}
