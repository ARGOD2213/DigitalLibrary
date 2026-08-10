package com.digitallibrary.engagement;

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

public class EngagementIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void addReview_AuthenticatedUser_ShouldCreateReview() throws Exception {
        AppUser user = createUser("reader@example.com", "ROLE_USER");
        Book book = createBook("Java Performance", "Technology", true);
        String token = tokenFor(user);

        CreateReviewRequest request = new CreateReviewRequest();
        request.setRating(5);
        request.setComment("Outstanding technical reference!");

        mockMvc.perform(post("/api/books/" + book.getId() + "/reviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.comment").value("Outstanding technical reference!"));
    }

    @Test
    void toggleFavorite_ShouldAddAndRemoveFavorite() throws Exception {
        AppUser user = createUser("favuser@example.com", "ROLE_USER");
        Book book = createBook("Design Patterns", "Technology", true);
        String token = tokenFor(user);

        // Add favorite
        mockMvc.perform(post("/api/books/" + book.getId() + "/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        // Check favorite status
        mockMvc.perform(get("/api/books/" + book.getId() + "/favorite/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));

        // Remove favorite
        mockMvc.perform(delete("/api/books/" + book.getId() + "/favorite")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
