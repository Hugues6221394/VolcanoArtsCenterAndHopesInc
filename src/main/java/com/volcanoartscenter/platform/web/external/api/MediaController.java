package com.volcanoartscenter.platform.web.external.api;

import com.volcanoartscenter.platform.shared.service.S3StorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/s3")
@RequiredArgsConstructor
@Slf4j
public class MediaController {

    private final S3StorageService s3StorageService;

    @GetMapping("/presign")
    public ResponseEntity<Map<String, String>> triggerPresignedUrl(
            @RequestParam String directory,
            @RequestParam String extension,
            @RequestParam String contentType) {
        
        try {
            // Validation: Only allow expected directories
            if (!directory.matches("^(products|talent|blogs|experiences|misc)$")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid upload directory."));
            }

            S3StorageService.PresignedUploadResult result = s3StorageService.generatePresignedUploadUrl(directory, extension, contentType);
            
            return ResponseEntity.ok(Map.of(
                "uploadUrl", result.uploadUrl(),
                "targetObjectKey", result.targetObjectKey()
            ));

        } catch (IllegalStateException e) {
            log.error("S3 Presigner error: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected presigning error", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "An error occurred generating the upload URL."));
        }
    }
}
