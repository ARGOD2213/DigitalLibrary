package com.digitallibrary.bootstrap;

import com.digitallibrary.entity.AppUser;
import com.digitallibrary.entity.Book;
import com.digitallibrary.enums.AccessType;
import com.digitallibrary.enums.ContentType;
import com.digitallibrary.enums.SubscriptionPlan;
import com.digitallibrary.enums.UserRole;
import com.digitallibrary.repository.AppUserRepository;
import com.digitallibrary.repository.BookRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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
        createUserIfMissing("Admin User", "admin@library.com", "admin123", UserRole.ADMIN, SubscriptionPlan.PREMIUM, null);
        createUserIfMissing("Reader User", "user@library.com", "user123", UserRole.USER, SubscriptionPlan.FREE, null);
        createUserIfMissing("Partner Publisher", "partner@library.com", "partner123", UserRole.PARTNER, SubscriptionPlan.PREMIUM, "Digital Partner Press");
        createContentIfMissing("Spring Boot Starter Guide", "Library Team", "Programming", "FREE-BOOK-001", ContentType.BOOK, AccessType.FREE);
        createContentIfMissing("Cloud Architecture Journal", "Digital Partner Press", "Cloud", "PAID-JOURNAL-001", ContentType.JOURNAL, AccessType.PAID);
        createContentIfMissing("Daily Tech Newspaper", "News Desk", "Technology", "FREE-NEWS-001", ContentType.NEWSPAPER, AccessType.FREE);
        createContentIfMissing("Advanced AWS Patterns", "Library Team", "Cloud", "PAID-BOOK-001", ContentType.BOOK, AccessType.PAID);
    }

    private void createUserIfMissing(String name, String email, String password, UserRole role,
                                     SubscriptionPlan plan, String organizationName) {
        if (appUserRepository.existsByEmail(email)) {
            return;
        }
        AppUser user = new AppUser(
                name,
                email,
                passwordEncoder.encode(password),
                role,
                plan,
                organizationName
        );
        appUserRepository.save(user);
    }

    private void createContentIfMissing(String title, String author, String category, String isbn,
                                        ContentType contentType, AccessType accessType) {
        if (bookRepository.existsByIsbn(isbn)) {
            return;
        }

        Book book = new Book(title, author, category, isbn, 5);
        book.setContentType(contentType);
        book.setAccessType(accessType);
        book.setPublisher(author);
        book.setDescription("Demo " + contentType.name().toLowerCase().replace('_', ' ') + " for learning library access rules.");
        book.setPreviewText("This preview is visible before a full paid/subscription flow is added.");
        bookRepository.save(book);
    }
}
