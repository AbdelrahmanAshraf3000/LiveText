package com.example.backend.repository;

import com.example.backend.entity.CollabState;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CollabStateRepository extends JpaRepository<CollabState, UUID> {
}