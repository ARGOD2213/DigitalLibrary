package com.digitallibrary.service.impl;

import com.digitallibrary.dto.*;
import com.digitallibrary.entity.*;
import com.digitallibrary.exception.ResourceNotFoundException;
import com.digitallibrary.repository.*;
import com.digitallibrary.service.LibraryEngagementService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class LibraryEngagementServiceImpl implements LibraryEngagementService {

    private final ReviewRepository reviewRepository;
    private final FavoriteRepository favoriteRepository;
    private final ReadingHistoryRepository readingHistoryRepository;
    private final BookRepository bookRepository;
    private final AppUserRepository appUserRepository;

    public LibraryEngagementServiceImpl(ReviewRepository reviewRepository,
                                        FavoriteRepository favoriteRepository,
                                        ReadingHistoryRepository readingHistoryRepository,
                                        BookRepository bookRepository,
                                        AppUserRepository appUserRepository) {
        this.reviewRepository = reviewRepository;
        this.favoriteRepository = favoriteRepository;
        this.readingHistoryRepository = readingHistoryRepository;
        this.bookRepository = bookRepository;
        this.appUserRepository = appUserRepository;
    }

    // ─── REVIEWS ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReviewResponse addReview(String userEmail, Long bookId, CreateReviewRequest request) {
        AppUser user = getUser(userEmail);
        Book book = getBook(bookId);

        if (reviewRepository.existsByUserIdAndBookId(user.getId(), bookId)) {
            // Update existing review
            Review existing = reviewRepository.findByUserIdAndBookId(user.getId(), bookId).get();
            existing.setRating(request.getRating());
            existing.setComment(request.getComment());
            Review saved = reviewRepository.save(existing);
            updateBookAverageRating(book);
            return ReviewResponse.fromEntity(saved);
        }

        Review review = new Review(user, book, request.getRating(), request.getComment());
        Review saved = reviewRepository.save(review);
        updateBookAverageRating(book);
        return ReviewResponse.fromEntity(saved);
    }

    @Override
    public PageResponse<ReviewResponse> getBookReviews(Long bookId, int page, int size) {
        Page<Review> reviews = reviewRepository.findByBookIdAndModeratedTrue(
                bookId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.fromPage(reviews.map(ReviewResponse::fromEntity));
    }

    @Override
    public PageResponse<ReviewResponse> getMyReviews(String userEmail, int page, int size) {
        AppUser user = getUser(userEmail);
        Page<Review> reviews = reviewRepository.findByUserId(
                user.getId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.fromPage(reviews.map(ReviewResponse::fromEntity));
    }

    @Override
    @Transactional
    public void deleteReview(String userEmail, Long reviewId) {
        AppUser user = getUser(userEmail);
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (!review.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("You can only delete your own reviews");
        }
        reviewRepository.delete(review);
        updateBookAverageRating(review.getBook());
    }

    // ─── FAVORITES ─────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void addFavorite(String userEmail, Long bookId) {
        AppUser user = getUser(userEmail);
        Book book = getBook(bookId);
        if (!favoriteRepository.existsByUserIdAndBookId(user.getId(), bookId)) {
            favoriteRepository.save(new Favorite(user, book));
        }
    }

    @Override
    @Transactional
    public void removeFavorite(String userEmail, Long bookId) {
        AppUser user = getUser(userEmail);
        favoriteRepository.deleteByUserIdAndBookId(user.getId(), bookId);
    }

    @Override
    public PageResponse<BookResponse> getMyFavorites(String userEmail, int page, int size) {
        AppUser user = getUser(userEmail);
        Page<Favorite> favorites = favoriteRepository.findByUserId(
                user.getId(), PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return PageResponse.fromPage(favorites.map(f -> BookResponse.fromEntity(f.getBook())));
    }

    @Override
    public boolean isFavorite(String userEmail, Long bookId) {
        AppUser user = getUser(userEmail);
        return favoriteRepository.existsByUserIdAndBookId(user.getId(), bookId);
    }

    // ─── READING HISTORY ───────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReadingHistoryResponse updateReadingProgress(String userEmail, Long bookId, int lastPage, BigDecimal progress) {
        AppUser user = getUser(userEmail);
        Book book = getBook(bookId);

        ReadingHistory rh = readingHistoryRepository.findByUserIdAndBookId(user.getId(), bookId)
                .orElseGet(() -> new ReadingHistory(user, book, lastPage, progress));

        rh.setLastPageRead(lastPage);
        rh.setProgressPercentage(progress);
        rh.setLastReadAt(LocalDateTime.now());

        // Increment book view count on first-time read
        if (rh.getId() == null) {
            book.setViewCount(book.getViewCount() + 1);
            bookRepository.save(book);
        }

        ReadingHistory saved = readingHistoryRepository.save(rh);
        return ReadingHistoryResponse.fromEntity(saved);
    }

    @Override
    public PageResponse<ReadingHistoryResponse> getMyReadingHistory(String userEmail, int page, int size) {
        AppUser user = getUser(userEmail);
        Page<ReadingHistory> history = readingHistoryRepository.findByUserIdOrderByLastReadAtDesc(
                user.getId(), PageRequest.of(page, size));
        return PageResponse.fromPage(history.map(ReadingHistoryResponse::fromEntity));
    }

    // ─── RECOMMENDATIONS ────────────────────────────────────────────────────────

    @Override
    public List<BookResponse> getRecommendations(String userEmail, int limit) {
        AppUser user = getUser(userEmail);

        // Get categories from user reading history
        List<String> readCategories = readingHistoryRepository.findReadCategoriesByUserId(user.getId());

        if (readCategories.isEmpty()) {
            // Fallback: return popular books
            return bookRepository.findPopularBooks(PageRequest.of(0, limit))
                    .stream()
                    .map(BookResponse::fromEntity)
                    .collect(Collectors.toList());
        }

        // Recommend published, non-deleted books from categories the user reads — ordered by rating
        String primaryCategory = readCategories.get(0);
        Page<Book> recommendations = bookRepository.findRecommendedByCategory(
                primaryCategory, PageRequest.of(0, limit));

        if (recommendations.isEmpty()) {
            return bookRepository.findPopularBooks(PageRequest.of(0, limit))
                    .stream()
                    .map(BookResponse::fromEntity)
                    .collect(Collectors.toList());
        }

        return recommendations.stream()
                .map(BookResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // ─── HELPERS ───────────────────────────────────────────────────────────────

    private AppUser getUser(String email) {
        return appUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private Book getBook(Long bookId) {
        return bookRepository.findByIdAndDeletedAtIsNull(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
    }

    private void updateBookAverageRating(Book book) {
        Double avg = reviewRepository.getAverageRatingByBookId(book.getId());
        if (avg != null) {
            book.setAverageRating(BigDecimal.valueOf(avg));
            bookRepository.save(book);
        }
    }
}
