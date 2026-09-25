package com.example.backend.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Two MinIO clients:
 * - {@code minioClient}: uses the INTERNAL endpoint for server-side operations (upload/download).
 * - {@code minioPresignClient}: uses the PUBLIC (browser-reachable) endpoint purely for
 *   generating presigned URLs so the signature host matches what the browser can resolve.
 */
@Configuration
@RequiredArgsConstructor
public class MinioConfig {

    private final MinioProperties props;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(props.endpoint())
                .credentials(props.accessKey(), props.secretKey())
                .build();
    }

    @Bean("minioPresignClient")
    public MinioClient minioPresignClient() {
        String publicEndpoint = props.publicEndpoint() != null && !props.publicEndpoint().isBlank()
                ? props.publicEndpoint() : props.endpoint();
        return MinioClient.builder()
                .endpoint(publicEndpoint)
                .credentials(props.accessKey(), props.secretKey())
                .build();
    }
}