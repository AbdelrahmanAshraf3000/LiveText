package com.example.backend.service;

import com.example.backend.collab.SessionRegistry;
import com.example.backend.entity.Document;
import com.example.backend.entity.DocumentVersion;
import com.example.backend.entity.User;
import com.example.backend.exception.PermissionDeniedException;
import com.example.backend.exception.VersionNotFoundException;
import com.example.backend.repository.DocumentRepository;
import com.example.backend.repository.DocumentVersionRepository;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VersionServiceTest {

    @Mock
    private DocumentVersionRepository versionRepo;
    @Mock
    private DocumentRepository documentRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private CollabStateService collabStateService;
    @Mock
    private SessionRegistry sessionRegistry;
    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private VersionService versionService;

    @Test
    void createSnapshot_savesVersionAndCollabState() {
        UUID docId = UUID.randomUUID();
        Document doc = docFixture(docId, 1L);
        User user = userFixture(1L);
        byte[] stateBytes = new byte[]{1, 2, 3};
        byte[] vectorBytes = new byte[]{0};

        when(documentRepo.findById(docId)).thenReturn(Optional.of(doc));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(versionRepo.findTopByDocumentIdOrderByVersionNoDesc(docId)).thenReturn(Optional.empty());
        when(versionRepo.save(any(DocumentVersion.class))).thenAnswer(inv -> {
            DocumentVersion v = inv.getArgument(0);
            v.setId(1L);
            return v;
        });

        DocumentVersion result = versionService.createSnapshot(docId, 1L, stateBytes, vectorBytes);

        assertThat(result.getVersionNo()).isEqualTo(1);
        assertThat(result.getByteSize()).isEqualTo(3);
        verify(collabStateService).saveSnapshot(eq(docId), eq(stateBytes), eq(vectorBytes));
    }

    @Test
    void createSnapshot_incrementsVersionNo() {
        UUID docId = UUID.randomUUID();
        Document doc = docFixture(docId, 1L);
        User user = userFixture(1L);
        DocumentVersion existing = new DocumentVersion();
        existing.setVersionNo(5);

        when(documentRepo.findById(docId)).thenReturn(Optional.of(doc));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(versionRepo.findTopByDocumentIdOrderByVersionNoDesc(docId)).thenReturn(Optional.of(existing));
        when(versionRepo.save(any(DocumentVersion.class))).thenAnswer(inv -> {
            DocumentVersion v = inv.getArgument(0);
            v.setId(2L);
            return v;
        });

        DocumentVersion result = versionService.createSnapshot(docId, 1L, new byte[]{1}, null);

        assertThat(result.getVersionNo()).isEqualTo(6);
    }

    @Test
    void listVersions_returnsVersions() {
        UUID docId = UUID.randomUUID();
        Document doc = docFixture(docId, 1L);
        when(documentRepo.findById(docId)).thenReturn(Optional.of(doc));
        when(versionRepo.findByDocumentIdOrderByVersionNoDesc(docId))
                .thenReturn(List.of());

        List<DocumentVersion> result = versionService.listVersions(docId);

        assertThat(result).isEmpty();
    }

    @Test
    void getVersion_found() {
        UUID docId = UUID.randomUUID();
        DocumentVersion version = new DocumentVersion();
        version.setVersionNo(3);
        when(versionRepo.findByDocumentIdAndVersionNo(docId, 3)).thenReturn(Optional.of(version));

        DocumentVersion result = versionService.getVersion(docId, 3);

        assertThat(result.getVersionNo()).isEqualTo(3);
    }

    @Test
    void getVersion_notFound_throws() {
        UUID docId = UUID.randomUUID();
        when(versionRepo.findByDocumentIdAndVersionNo(docId, 99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> versionService.getVersion(docId, 99))
                .isInstanceOf(VersionNotFoundException.class);
    }

    @Test
    void rollback_closesSessionsAndReplacesState() {
        UUID docId = UUID.randomUUID();
        Document doc = docFixture(docId, 1L);
        User user = userFixture(1L);
        DocumentVersion target = new DocumentVersion();
        target.setVersionNo(2);
        target.setYjsState(new byte[]{10, 20, 30});
        target.setByteSize(3);

        when(versionRepo.findByDocumentIdAndVersionNo(docId, 2)).thenReturn(Optional.of(target));
        when(versionRepo.findTopByDocumentIdOrderByVersionNoDesc(docId)).thenReturn(Optional.of(target));
        when(documentRepo.findById(docId)).thenReturn(Optional.of(doc));
        when(userRepo.findById(1L)).thenReturn(Optional.of(user));
        when(versionRepo.save(any(DocumentVersion.class))).thenAnswer(inv -> {
            DocumentVersion v = inv.getArgument(0);
            v.setId(10L);
            return v;
        });

        DocumentVersion result = versionService.rollback(docId, 2, 1L);

        verify(permissionService).requireCanEdit(docId, 1L);
        verify(sessionRegistry).closeAllSessions(docId);
        verify(collabStateService).rollbackToState(eq(docId), any(byte[].class));
        assertThat(result.getVersionNo()).isEqualTo(3);
    }

    @Test
    void rollback_viewer_throws() {
        UUID docId = UUID.randomUUID();
        when(permissionService.requireCanEdit(docId, 3L))
                .thenThrow(new PermissionDeniedException("Editor access required"));

        assertThatThrownBy(() -> versionService.rollback(docId, 1, 3L))
                .isInstanceOf(PermissionDeniedException.class);
    }

    private Document docFixture(UUID id, Long ownerId) {
        Document doc = Document.createNew("Doc " + id, ownerId);
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