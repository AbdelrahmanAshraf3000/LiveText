package com.example.backend.service;

import com.example.backend.entity.Document;
import com.example.backend.entity.DocumentPermission;
import com.example.backend.entity.Role;
import com.example.backend.entity.User;
import com.example.backend.exception.DuplicatePermissionException;
import com.example.backend.exception.PermissionDeniedException;
import com.example.backend.repository.DocumentPermissionRepository;
import com.example.backend.repository.DocumentRepository;
import com.example.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PermissionServiceTest {

    @Mock
    private DocumentRepository documentRepository;
    @Mock
    private DocumentPermissionRepository permissionRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private PermissionService permissionService;

    @Test
    void getRole_owner_returnsOwner() {
        UUID docId = UUID.randomUUID();
        Document doc = docFixture(docId, 1L);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));

        Role role = permissionService.getRole(docId, 1L);

        assertThat(role).isEqualTo(Role.OWNER);
    }

    @Test
    void getRole_editor_returnsEditor() {
        UUID docId = UUID.randomUUID();
        Document doc = docFixture(docId, 1L);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(permissionRepository.findByDocumentIdAndUserId(docId, 2L))
                .thenReturn(Optional.of(new DocumentPermission(doc, userFixture(2L), Role.EDITOR, userFixture(1L))));

        Role role = permissionService.getRole(docId, 2L);

        assertThat(role).isEqualTo(Role.EDITOR);
    }

    @Test
    void getRole_noAccess_returnsNull() {
        UUID docId = UUID.randomUUID();
        Document doc = docFixture(docId, 1L);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(permissionRepository.findByDocumentIdAndUserId(docId, 99L)).thenReturn(Optional.empty());

        Role role = permissionService.getRole(docId, 99L);

        assertThat(role).isNull();
    }

    @Test
    void requireAccess_noAccess_throws() {
        UUID docId = UUID.randomUUID();
        Document doc = docFixture(docId, 1L);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(permissionRepository.findByDocumentIdAndUserId(docId, 99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> permissionService.requireAccess(docId, 99L))
                .isInstanceOf(PermissionDeniedException.class);
    }

    @Test
    void share_byUsername_succeeds() {
        UUID docId = UUID.randomUUID();
        Document doc = docFixture(docId, 1L);
        User grantor = userFixture(1L);
        User target = userFixture(2L);
        target.setUsername("bob");
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(userRepository.findById(1L)).thenReturn(Optional.of(grantor));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(target));
        when(permissionRepository.existsByDocumentIdAndUserId(docId, 2L)).thenReturn(false);
        when(permissionRepository.save(any(DocumentPermission.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentPermission perm = permissionService.share(docId, 1L, "bob", Role.EDITOR);

        assertThat(perm.getRole()).isEqualTo(Role.EDITOR);
        assertThat(perm.getUser().getUsername()).isEqualTo("bob");
    }

    @Test
    void share_duplicate_throws() {
        UUID docId = UUID.randomUUID();
        Document doc = docFixture(docId, 1L);
        User grantor = userFixture(1L);
        User target = userFixture(2L);
        target.setUsername("bob");
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(userRepository.findById(1L)).thenReturn(Optional.of(grantor));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(target));
        when(permissionRepository.existsByDocumentIdAndUserId(docId, 2L)).thenReturn(true);

        assertThatThrownBy(() -> permissionService.share(docId, 1L, "bob", Role.EDITOR))
                .isInstanceOf(DuplicatePermissionException.class);
    }

    @Test
    void share_withOwner_throws() {
        UUID docId = UUID.randomUUID();
        Document doc = docFixture(docId, 1L);
        User grantor = userFixture(1L);
        User target = userFixture(1L);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(userRepository.findById(1L)).thenReturn(Optional.of(grantor));
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(target));

        assertThatThrownBy(() -> permissionService.share(docId, 1L, "alice", Role.EDITOR))
                .isInstanceOf(DuplicatePermissionException.class);
    }

    @Test
    void changeRole_updatesExistingPermission() {
        UUID docId = UUID.randomUUID();
        Document doc = docFixture(docId, 1L);
        User target = userFixture(2L);
        target.setUsername("bob");
        DocumentPermission existing = new DocumentPermission(doc, target, Role.VIEWER, userFixture(1L));
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(target));
        when(permissionRepository.findByDocumentIdAndUserId(docId, 2L)).thenReturn(Optional.of(existing));
        when(permissionRepository.save(any(DocumentPermission.class))).thenAnswer(inv -> inv.getArgument(0));

        DocumentPermission updated = permissionService.changeRole(docId, 1L, "bob", Role.EDITOR);

        assertThat(updated.getRole()).isEqualTo(Role.EDITOR);
    }

    @Test
    void revoke_removesPermission() {
        UUID docId = UUID.randomUUID();
        Document doc = docFixture(docId, 1L);
        User target = userFixture(2L);
        target.setUsername("bob");
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(target));

        permissionService.revoke(docId, 1L, "bob");

        verify(permissionRepository).deleteByDocumentIdAndUserId(docId, 2L);
    }

    @Test
    void requireCanEdit_viewer_throws() {
        UUID docId = UUID.randomUUID();
        Document doc = docFixture(docId, 1L);
        when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
        when(permissionRepository.findByDocumentIdAndUserId(docId, 3L))
                .thenReturn(Optional.of(new DocumentPermission(doc, userFixture(3L), Role.VIEWER, userFixture(1L))));

        assertThatThrownBy(() -> permissionService.requireCanEdit(docId, 3L))
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