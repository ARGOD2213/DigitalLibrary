package com.digitallibrary.bootstrap;

import com.digitallibrary.entity.AppUser;
import com.digitallibrary.entity.Book;
import com.digitallibrary.enums.UserRole;
import com.digitallibrary.repository.AppUserRepository;
import com.digitallibrary.repository.BookRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final BookRepository bookRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(AppUserRepository appUserRepository, BookRepository bookRepository,
                           PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.bookRepository = bookRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        createUserIfMissing("admin", "admin@library.com", "admin123", "Admin User", UserRole.ROLE_ADMIN);
        createUserIfMissing("user", "user@library.com", "user123", "Reader User", UserRole.ROLE_USER);
        createUserIfMissing("vendor", "vendor@library.com", "vendor123", "Partner Publisher", UserRole.ROLE_VENDOR);

        createContentIfMissing("Spring Boot Starter Guide", "Library Team", "Programming", "FREE-BOOK-001", true, BigDecimal.ZERO);
        createContentIfMissing("Cloud Architecture Journal", "Digital Partner Press", "Cloud", "PAID-JOURNAL-001", false, new BigDecimal("499.00"));
        createContentIfMissing("Daily Tech Newspaper", "News Desk", "Technology", "FREE-NEWS-001", true, BigDecimal.ZERO);
        createContentIfMissing("Advanced AWS Patterns", "Library Team", "Cloud", "PAID-BOOK-001", false, new BigDecimal("799.00"));
    }

    private void createUserIfMissing(String username, String email, String password, String fullName, UserRole role) {
        if (appUserRepository.existsByEmail(email)) {
            return;
        }
        AppUser user = new AppUser(
                username,
                email,
                passwordEncoder.encode(password),
                fullName,
                role
        );
        appUserRepository.save(user);
    }

    private void createContentIfMissing(String title, String author, String category, String isbn,
                                        boolean isFree, BigDecimal price) {
        if (bookRepository.existsByIsbn(isbn)) {
            return;
        }

        Book book = new Book(title, author, category, isbn, 5);
        book.setFree(isFree);
        book.setPrice(price);
        book.setDescription("Demo book content for learning library access rules.");
        bookRepository.save(book);
    }
}
