package com.example.backend.service;

import com.example.backend.entity.Document;
import com.example.backend.entity.DocumentPermission;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.exception.PermissionDeniedException;
import com.example.backend.repository.DocumentPermissionRepository;
import com.example.backend.repository.DocumentRepository;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentPermissionRepository permissionRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private DocumentService documentService;

    @Test
    void create_savesDocument() {
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document doc = documentService.create("My Doc", 1L);

        assertThat(doc.getTitle()).isEqualTo("My Doc");
        assertThat(doc.getOwnerId()).isEqualTo(1L);
        assertThat(doc.getId()).isNotNull();
    }

    @Test
    void getForUser_owner_seesOwnDoc() {
        UUID docId = UUID.randomUUID();
        Document doc = docFixture(docId, 1L);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        Document result = documentService.getForUser(docId, 1L);

        assertThat(result.getId()).isEqualTo(docId);
    }

    @Test
    void getForUser_sharedUser_seesDoc() {
        UUID docId = UUID.randomUUID();
        Document doc = docFixture(docId, 1L);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(permissionRepository.findByDocumentIdAndUserId(docId, 2L))
                .thenReturn(Optional.of(new DocumentPermission(doc, userFixture(2L), Role.EDITOR, userFixture(1L))));

        Document result = documentService.getForUser(docId, 2L);

        assertThat(result.getId()).isEqualTo(docId);
    }

    @Test
    void getForUser_unauthorized_throws() {
        UUID docId = UUID.randomUUID();
        Document doc = docFixture(docId, 1L);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(permissionRepository.findByDocumentIdAndUserId(docId, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> documentService.getForUser(docId, 99L))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void rename_editor_succeeds() {
        UUID docId = UUID.randomUUID();
        Document doc = docFixture(docId, 1L);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(permissionRepository.findByDocumentIdAndUserId(docId, 2L))
                .thenReturn(Optional.of(new DocumentPermission(doc, userFixture(2L), Role.EDITOR, userFixture(1L))));
        when(documentRepository.save(any(Document.class))).thenAnswer(inv -> inv.getArgument(0));

        Document renamed = documentService.rename(docId, 2L, "New Title");

        assertThat(renamed.getTitle()).isEqualTo("New Title");
    }

    @Test
    void rename_viewer_throws() {
        UUID docId = UUID.randomUUID();
        Document doc = docFixture(docId, 1L);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(permissionRepository.findByDocumentIdAndUserId(docId, 3L))
                .thenReturn(Optional.of(new DocumentPermission(doc, userFixture(3L), Role.VIEWER, userFixture(1L))));

        assertThatThrownBy(() -> documentService.rename(docId, 3L, "New Title"))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void delete_owner_succeeds() {
        UUID docId = UUID.randomUUID();
        Document doc = docFixture(docId, 1L);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        documentService.delete(docId, 1L);

        verify(documentRepository).delete(doc);
    }

    @Test
    void delete_nonOwner_throws() {
        UUID docId = UUID.randomUUID();
        Document doc = docFixture(docId, 1L);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        assertThatThrownBy(() -> documentService.delete(docId, 2L))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void listOwned_returnsOwnerDocs() {
        when(documentRepository.findByOwnerIdOrderByUpdatedAtDesc(1L))
                .thenReturn(List.of(docFixture(UUID.randomUUID(), 1L)));

        List<Document> docs = documentService.listOwned(1L);

        assertThat(docs).hasSize(1);
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