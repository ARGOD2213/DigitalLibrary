package com.digitallibrary.service;

import com.digitallibrary.dto.*;

import java.math.BigDecimal;
import java.util.List;

public interface LibraryEngagementService {
    // Reviews
    ReviewResponse addReview(String userEmail, Long bookId, CreateReviewRequest request);
    PageResponse<ReviewResponse> getBookReviews(Long bookId, int page, int size);
    PageResponse<ReviewResponse> getMyReviews(String userEmail, int page, int size);
    void deleteReview(String userEmail, Long reviewId);

    // Favorites
    void addFavorite(String userEmail, Long bookId);
    void removeFavorite(String userEmail, Long bookId);
    PageResponse<BookResponse> getMyFavorites(String userEmail, int page, int size);
    boolean isFavorite(String userEmail, Long bookId);

    // Reading History
    ReadingHistoryResponse updateReadingProgress(String userEmail, Long bookId, int lastPage, BigDecimal progress);
    PageResponse<ReadingHistoryResponse> getMyReadingHistory(String userEmail, int page, int size);

    // Recommendations
    List<BookResponse> getRecommendations(String userEmail, int limit);
}
