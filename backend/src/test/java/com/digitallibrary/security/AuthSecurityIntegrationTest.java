package com.digitallibrary.security;

import com.digitallibrary.BaseIntegrationTest;
import com.digitallibrary.dto.*;
import com.digitallibrary.entity.*;
import com.digitallibrary.enums.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuthSecurityIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void healthCheck_ShouldReturn200OK() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void login_WithValidCredentials_ShouldReturnAccessAndRefreshToken() throws Exception {
        createUser("user@example.com", "ROLE_USER");

        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("Test@1234");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    void login_WithInvalidPassword_ShouldReturn401Unauthorized() throws Exception {
        createUser("user@example.com", "ROLE_USER");

        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("WrongPassword");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedAdminRoute_WithoutToken_ShouldReturn401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/admin/audit-logs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedAdminRoute_WithUserToken_ShouldReturn403Forbidden() throws Exception {
        AppUser user = createUser("regularuser@example.com", "ROLE_USER");
        String userToken = tokenFor(user);

        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("Authorization", "Bearer " + userToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void protectedAdminRoute_WithAdminToken_ShouldReturn200OK() throws Exception {
        AppUser admin = createUser("adminuser@example.com", "ROLE_ADMIN");
        String adminToken = tokenFor(admin);

        mockMvc.perform(get("/api/admin/audit-logs")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
