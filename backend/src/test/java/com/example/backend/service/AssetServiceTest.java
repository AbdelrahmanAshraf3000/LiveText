package com.example.backend.service;

import com.example.backend.config.MinioProperties;
import com.example.backend.entity.Asset;
import com.example.backend.entity.Document;
import com.example.backend.entity.User;
import com.example.backend.repository.AssetRepository;
import com.example.backend.repository.DocumentRepository;
import com.example.backend.repository.UserRepository;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssetServiceTest {

    @Mock
    private AssetRepository assetRepo;
    @Mock
    private DocumentRepository documentRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private PermissionService permissionService;
    @Mock
    private MinioClient minioClient;
    @Mock
    private MinioClient minioPresignClient;

    private AssetService assetService;

    @BeforeEach
    void setUp() {
        MinioProperties props = new MinioProperties(
                "http://localhost:9000", "http://localhost:9000",
                "test", "test", "test-bucket", Duration.ofMinutes(10));
        assetService = new AssetService(assetRepo, documentRepo, userRepo,
                permissionService, minioClient, minioPresignClient, props);
    }

    @Test
    void upload_validImage_persistsAndUploads() throws Exception {
        UUID docId = UUID.randomUUID();
        Long userId = 1L;
        Document doc = docFixture(docId, userId);
        User uploader = userFixture(userId);
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.png", "image/png", new byte[]{1, 2, 3});

        when(permissionService.requireCanEdit(docId, userId)).thenReturn(com.example.backend.entity.Role.EDITOR);
        when(documentRepo.findById(docId)).thenReturn(Optional.of(doc));
        when(userRepo.findById(userId)).thenReturn(Optional.of(uploader));
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
        when(assetRepo.save(any(Asset.class))).thenAnswer(inv -> inv.getArgument(0));

        Asset result = assetService.upload(docId, userId, file);

        assertThat(result.getMimeType()).isEqualTo("image/png");
        assertThat(result.getByteSize()).isEqualTo(3L);
        assertThat(result.getObjectKey()).startsWith(docId + "/");
        assertThat(result.getUrl()).startsWith("/api/assets/");
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    void upload_nonImage_throws() {
        UUID docId = UUID.randomUUID();
        Long userId = 1L;
        when(permissionService.requireCanEdit(docId, userId)).thenReturn(com.example.backend.entity.Role.EDITOR);
        when(documentRepo.findById(docId)).thenReturn(Optional.of(docFixture(docId, userId)));
        when(userRepo.findById(userId)).thenReturn(Optional.of(userFixture(userId)));
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.txt", "text/plain", new byte[]{1});

        assertThatThrownBy(() -> assetService.upload(docId, userId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Only image");
    }

    @Test
    void upload_tooLarge_throws() {
        UUID docId = UUID.randomUUID();
        Long userId = 1L;
        when(permissionService.requireCanEdit(docId, userId)).thenReturn(com.example.backend.entity.Role.EDITOR);
        when(documentRepo.findById(docId)).thenReturn(Optional.of(docFixture(docId, userId)));
        when(userRepo.findById(userId)).thenReturn(Optional.of(userFixture(userId)));
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.png", "image/png", new byte[5_000_001]);

        assertThatThrownBy(() -> assetService.upload(docId, userId, file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("5MB");
    }

    @Test
    void getPresignedUrl_returnsUrl() throws Exception {
        UUID assetId = UUID.randomUUID();
        Asset asset = new Asset();
        asset.setId(assetId);
        asset.setObjectKey("doc/abc.png");
        asset.setUrl("/api/assets/" + assetId);

        when(assetRepo.findById(assetId)).thenReturn(Optional.of(asset));
        lenient().when(minioPresignClient.getPresignedObjectUrl(any()))
                .thenReturn("http://localhost:9000/test-bucket/doc/abc.png?sig=xyz");

        String url = assetService.getPresignedUrl(assetId);

        assertThat(url).contains("test-bucket").contains("doc/abc.png");
    }

    private Document docFixture(UUID id, Long ownerId) {
        Document doc = Document.createNew("Doc", ownerId);
        doc.setId(id);
        return doc;
    }

    private User userFixture(Long id) {
        User u = new User();
        u.setId(id);
        u.setUsername("user" + id);
        return u;
    }
}