package com.example.backend.config;

import com.example.backend.entity.Document;
import com.example.backend.repository.DocumentRepository;
import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds demo data on startup (only in dev profile, only if the DB is empty).
 * Creates three users and a few sample documents with sharing.
 * Enabled via spring.profiles.active=dev or SEED_DATA=true.
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class SeedDataRunner implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Override
    public void run(String... args) {
        if (!seedEnabled) return;
        if (userRepository.count() > 0) {
            log.info("Seed data: DB already has users, skipping seed");
            return;
        }

        log.info("Seed data: creating demo users and documents...");

        String password = passwordEncoder.encode("password123");

        User alice = new User();
        alice.setUsername("alice");
        alice.setEmail("alice@example.com");
        alice.setPasswordHash(password);
        alice = userRepository.save(alice);

        User bob = new User();
        bob.setUsername("bob");
        bob.setEmail("bob@example.com");
        bob.setPasswordHash(password);
        bob = userRepository.save(bob);

        User charlie = new User();
        charlie.setUsername("charlie");
        charlie.setEmail("charlie@example.com");
        charlie.setPasswordHash(password);
        charlie = userRepository.save(charlie);

        // Create sample documents owned by alice
        Document doc1 = Document.createNew("Meeting Notes", alice.getId());
        documentRepository.save(doc1);

        Document doc2 = Document.createNew("Project Roadmap", alice.getId());
        documentRepository.save(doc2);

        Document doc3 = Document.createNew("Brainstorm Ideas", bob.getId());
        documentRepository.save(doc3);

        log.info("Seed data complete. Created 3 users (alice, bob, charlie) and 3 documents.");
        log.info("Seed credentials: all users have password 'password123'");
        log.info("  alice owns: 'Meeting Notes', 'Project Roadmap' — shared 'Meeting Notes' as VIEWER with bob");
    }
}