package com.example.backend.controller;

import com.example.backend.entity.User;
import com.example.backend.repository.UserRepository;
import com.example.backend.service.JwtService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Transactional
class DocumentControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private JwtService jwtService;

    private String registerAndGetToken(String username) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(username + "@itest.com");
        user.setPasswordHash("dummy");
        userRepository.save(user);
        return jwtService.generate(user.getId(), username).accessToken();
    }

    @Test
    void create_thenList_owned() throws Exception {
        String token = registerAndGetToken("docowner");

        mockMvc.perform(post("/api/documents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Integration Test Doc"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Integration Test Doc"))
                .andExpect(jsonPath("$.data.viewerRole").value("OWNER"));

        mockMvc.perform(get("/api/documents/owned")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Integration Test Doc"));
    }

    @Test
    void rename_document() throws Exception {
        String token = registerAndGetToken("renameuser");
        String docId = createDoc(token, "Original Title");

        mockMvc.perform(patch("/api/documents/" + docId + "/rename")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Renamed Title"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("Renamed Title"));
    }

    @Test
    void delete_document_byOwner() throws Exception {
        String token = registerAndGetToken("deleteuser");
        String docId = createDoc(token, "To Delete");

        mockMvc.perform(delete("/api/documents/" + docId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void open_withoutAuth_returns401() throws Exception {
        mockMvc.perform(get("/api/documents/" + UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void share_byUsername_thenList() throws Exception {
        String ownerToken = registerAndGetToken("shareowner");
        String viewerToken = registerAndGetToken("shareviewer");
        String docId = createDoc(ownerToken, "Shared Doc");

        mockMvc.perform(post("/api/documents/" + docId + "/permissions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"shareviewer","role":"VIEWER"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("shareviewer"));

        mockMvc.perform(get("/api/documents/shared")
                        .header("Authorization", "Bearer " + viewerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("Shared Doc"))
                .andExpect(jsonPath("$.data[0].viewerRole").value("VIEWER"));
    }

    @Test
    void viewer_cannotRename() throws Exception {
        String ownerToken = registerAndGetToken("protowner");
        String viewerToken = registerAndGetToken("protviewer");
        String docId = createDoc(ownerToken, "Protected Doc");

        mockMvc.perform(post("/api/documents/" + docId + "/permissions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"protviewer","role":"VIEWER"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/documents/" + docId + "/rename")
                        .header("Authorization", "Bearer " + viewerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"Hacked Title"}
                                """))
                .andExpect(status().isForbidden());
    }

    private String createDoc(String token, String title) throws Exception {
        String response = mockMvc.perform(post("/api/documents")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"" + title + "\"}"))
                .andReturn().getResponse().getContentAsString();

        JsonNode root = new ObjectMapper().readTree(response);
        return root.get("data").get("id").asText();
    }
}