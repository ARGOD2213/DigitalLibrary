package com.digitallibrary;

import com.digitallibrary.entity.*;
import com.digitallibrary.repository.*;
import com.digitallibrary.security.*;
import com.digitallibrary.dto.*;
import com.digitallibrary.enums.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base integration test class — starts full Spring context with H2,
 * provides helpers for seeding users, books, and generating JWT tokens.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected AppUserRepository appUserRepository;
    @Autowired protected BookRepository bookRepository;
    @Autowired protected ReviewRepository reviewRepository;
    @Autowired protected FavoriteRepository favoriteRepository;
    @Autowired protected ReadingHistoryRepository readingHistoryRepository;
    @Autowired protected RefreshTokenRepository refreshTokenRepository;
    @Autowired protected AuditLogRepository auditLogRepository;
    @Autowired protected PaymentRepository paymentRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected JwtService jwtService;

    @BeforeEach
    void cleanDatabase() {
        reviewRepository.deleteAll();
        favoriteRepository.deleteAll();
        readingHistoryRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        auditLogRepository.deleteAll();
        paymentRepository.deleteAll();
        bookRepository.deleteAll();
        appUserRepository.deleteAll();
    }

    protected AppUser createUser(String email, String roleStr) {
        AppUser user = new AppUser();
        user.setUsername(email.split("@")[0] + "_" + System.currentTimeMillis());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("Test@1234"));
        user.setFullName("Test User");
        user.setRole(com.digitallibrary.enums.UserRole.valueOf(roleStr));
        user.setPhoneNumber("9999999999");
        user.setStatus("ACTIVE");
        return appUserRepository.save(user);
    }

    protected String tokenFor(AppUser user) {
        org.springframework.security.core.userdetails.User ud =
                new org.springframework.security.core.userdetails.User(
                        user.getEmail(), user.getPassword(),
                        java.util.Collections.singletonList(
                                new org.springframework.security.core.authority.SimpleGrantedAuthority(user.getRole().name())
                        )
                );
        return jwtService.generateToken(ud);
    }

    protected Book createBook(String title, String category, boolean published) {
        Book book = new Book(title, "Test Author", category, null, 200);
        book.setDescription("Test description");
        book.setPublished(published);
        book.setStatus(published ? "PUBLISHED" : "DRAFT");
        book.setFree(true);
        return bookRepository.save(book);
    }
}
