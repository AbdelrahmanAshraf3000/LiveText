package com.example.backend.service;

import com.example.backend.config.MinioProperties;
import com.example.backend.entity.Asset;
import com.example.backend.entity.Document;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.exception.DocumentNotFoundException;
import com.example.backend.repository.AssetRepository;
import com.example.backend.repository.DocumentRepository;
import com.example.backend.repository.UserRepository;
import io.minio.BucketExistsArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Asset storage via MinIO (backend-proxied upload + presigned-URL redirect for serving).
 * <p>
 * Upload: browser -> backend -> MinIO (no browser CORS needed).
 * Download: browser -> backend -> 302 redirect to a presigned MinIO GET URL
 * (stable "/api/assets/{id}" URL stored in the document; fresh signed URL on every request).
 */
@Slf4j
@Service
public class AssetService {

    private static final long MAX_IMAGE_SIZE = 5_000_000L;

    private final AssetRepository assetRepo;
    private final DocumentRepository documentRepo;
    private final UserRepository userRepo;
    private final PermissionService permissionService;
    private final MinioClient minioClient;
    private final MinioClient minioPresignClient;
    private final MinioProperties props;

    private final AtomicBoolean bucketInitialized = new AtomicBoolean(false);

    public AssetService(AssetRepository assetRepo,
                        DocumentRepository documentRepo,
                        UserRepository userRepo,
                        PermissionService permissionService,
                        MinioClient minioClient,
                        @Qualifier("minioPresignClient") MinioClient minioPresignClient,
                        MinioProperties props) {
        this.assetRepo = assetRepo;
        this.documentRepo = documentRepo;
        this.userRepo = userRepo;
        this.permissionService = permissionService;
        this.minioClient = minioClient;
        this.minioPresignClient = minioPresignClient;
        this.props = props;
    }

    @Transactional
    public Asset upload(UUID documentId, Long uploaderId, MultipartFile file) throws IOException {
        Role role = permissionService.requireCanEdit(documentId, uploaderId);

        Document doc = documentRepo.findById(documentId)
                .orElseThrow(DocumentNotFoundException::new);
        User uploader = userRepo.findById(uploaderId)
                .orElseThrow(() -> new IllegalStateException("User not found: " + uploaderId));

        String mime = file.getContentType();
        if (mime == null || !mime.startsWith("image/")) {
            throw new IllegalArgumentException("Only image files are allowed");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new IllegalArgumentException("Image size must not exceed 5MB");
        }

        ensureBucket();

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "image";
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < originalName.length() - 1) {
            extension = originalName.substring(dotIndex);
        }
        String objectKey = documentId + "/" + UUID.randomUUID() + extension;

        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(props.bucket())
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(mime)
                            .build());
        } catch (Exception e) {
            log.error("Failed to upload to MinIO for doc {}: {}", documentId, e.getMessage(), e);
            throw new IOException("Failed to store image", e);
        }

        Asset asset = new Asset();
        asset.setDocument(doc);
        asset.setUploader(uploader);
        asset.setFileName(originalName);
        asset.setMimeType(mime);
        asset.setByteSize(file.getSize());
        asset.setObjectKey(objectKey);
        asset.setUrl("/api/assets/" + asset.getId());
        asset = assetRepo.save(asset);

        log.info("Uploaded asset {} ({} bytes) to MinIO key {}", asset.getId(), file.getSize(), objectKey);
        return asset;
    }

    /**
     * Generate a short-lived presigned GET URL for the asset's MinIO object.
     * The stored document URL ("/api/assets/{id}") redirects to this fresh URL on every request.
     */
    @Transactional(readOnly = true)
    public String getPresignedUrl(UUID assetId) {
        Asset asset = getById(assetId);
        return getPresignedUrl(asset);
    }

    public String getPresignedUrl(Asset asset) {
        try {
            return minioPresignClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(props.bucket())
                            .object(asset.getObjectKey())
                            .expiry((int) props.presignExpiry().getSeconds(), TimeUnit.SECONDS)
                            .build());
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for asset {}: {}", asset.getId(), e.getMessage(), e);
            throw new IllegalStateException("Failed to generate download URL", e);
        }
    }

    @Transactional(readOnly = true)
    public Asset getById(UUID assetId) {
        return assetRepo.findById(assetId)
                .orElseThrow(() -> new IllegalArgumentException("Asset not found: " + assetId));
    }

    private void ensureBucket() {
        if (bucketInitialized.get()) {
            return;
        }
        synchronized (this) {
            if (bucketInitialized.get()) {
                return;
            }
            try {
                boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(props.bucket()).build());
                if (!exists) {
                    minioClient.makeBucket(MakeBucketArgs.builder().bucket(props.bucket()).build());
                    log.info("Created MinIO bucket '{}'", props.bucket());
                }
                bucketInitialized.set(true);
            } catch (Exception e) {
                // Non-fatal: bucket may already exist or MinIO is momentarily down. Retry on next upload.
                log.warn("Could not ensure MinIO bucket '{}' (continuing): {}", props.bucket(), e.getMessage());
            }
        }
    }
}