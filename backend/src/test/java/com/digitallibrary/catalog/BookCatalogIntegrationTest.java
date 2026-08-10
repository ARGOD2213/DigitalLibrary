package com.digitallibrary.catalog;

import com.digitallibrary.BaseIntegrationTest;
import com.digitallibrary.dto.*;
import com.digitallibrary.entity.*;
import com.digitallibrary.enums.*;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class BookCatalogIntegrationTest extends BaseIntegrationTest {

    @Test
    void getPublicBooks_ShouldReturnOnlyPublishedBooks() throws Exception {
        createBook("Published Java Guide", "Technology", true);
        createBook("Draft Unreleased Book", "Technology", false);

        mockMvc.perform(get("/api/books"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Published Java Guide"));
    }

    @Test
    void searchBooks_ByKeyword_ShouldReturnMatchingBook() throws Exception {
        createBook("Spring Boot Architecture", "Technology", true);
        createBook("Cooking Masterclass", "Food", true);

        mockMvc.perform(get("/api/books/search")
                        .param("keyword", "Spring"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("Spring Boot Architecture"));
    }

    @Test
    void togglePublish_WithAdminUser_ShouldUpdateStatus() throws Exception {
        Book draftBook = createBook("Draft Book", "Technology", false);
        AppUser admin = createUser("admin@library.com", "ROLE_ADMIN");
        String adminToken = tokenFor(admin);

        mockMvc.perform(patch("/api/books/" + draftBook.getId() + "/publish")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.published").value(true));
    }
}
