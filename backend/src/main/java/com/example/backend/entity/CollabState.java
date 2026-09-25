package com.example.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "collab_state")
@Getter
@Setter
@NoArgsConstructor
public class CollabState {

    @Id
    private java.util.UUID documentId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "document_id")
    private Document document;

    @Column(name = "snapshot")
    private byte[] snapshot;

    @Column(name = "state_vector")
    private byte[] stateVector;

    @Column(name = "snapshot_at")
    private LocalDateTime snapshotAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}