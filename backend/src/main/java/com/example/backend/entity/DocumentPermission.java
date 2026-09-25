package com.example.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "document_permissions",
        uniqueConstraints = @UniqueConstraint(name = "uq_doc_user", columnNames = {"document_id", "user_id"}))
@Getter
@Setter
@NoArgsConstructor
public class DocumentPermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @ManyToOne
    @JoinColumn(name = "granted_by", nullable = false)
    private User grantedBy;

    @Column(name = "granted_at", nullable = false, updatable = false)
    private LocalDateTime grantedAt;

    public DocumentPermission(Document document, User user, Role role, User grantedBy) {
        this.document = document;
        this.user = user;
        this.role = role;
        this.grantedBy = grantedBy;
    }

    @PrePersist
    void onGrant() {
        this.grantedAt = LocalDateTime.now();
    }
}