package com.example.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.minio")
public record MinioProperties(
        String endpoint,        // internal endpoint used for server-side IO (e.g. http://minio:9000)
        String publicEndpoint,  // browser-reachable endpoint used for presigned URLs (e.g. http://localhost:9000)
        String accessKey,
        String secretKey,
        String bucket,
        Duration presignExpiry) {  // expiry for presigned GET URLs (default 10m)
}