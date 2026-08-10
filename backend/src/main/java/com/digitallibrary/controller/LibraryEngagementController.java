package com.digitallibrary.controller;

import com.digitallibrary.dto.*;
import com.digitallibrary.service.LibraryEngagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class LibraryEngagementController {

    private final LibraryEngagementService engagementService;

    public LibraryEngagementController(LibraryEngagementService engagementService) {
        this.engagementService = engagementService;
    }

    // ─── REVIEWS ───────────────────────────────────────────────────────────────

    @PostMapping("/books/{bookId}/reviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReviewResponse>> addReview(
            @PathVariable Long bookId,
            @Valid @RequestBody CreateReviewRequest request,
            Principal principal) {
        ReviewResponse review = engagementService.addReview(principal.getName(), bookId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted successfully", review));
    }

    @GetMapping("/books/{bookId}/reviews")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getBookReviews(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<ReviewResponse> reviews = engagementService.getBookReviews(bookId, page, size);
        return ResponseEntity.ok(ApiResponse.success("Reviews retrieved", reviews));
    }

    @GetMapping("/users/me/reviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getMyReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {
        PageResponse<ReviewResponse> reviews = engagementService.getMyReviews(principal.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.success("My reviews retrieved", reviews));
    }

    @DeleteMapping("/reviews/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> deleteReview(
            @PathVariable Long reviewId,
            Principal principal) {
        engagementService.deleteReview(principal.getName(), reviewId);
        return ResponseEntity.ok(ApiResponse.success("Review deleted", null));
    }

    // ─── FAVORITES ─────────────────────────────────────────────────────────────

    @PostMapping("/books/{bookId}/favorite")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> addFavorite(
            @PathVariable Long bookId,
            Principal principal) {
        engagementService.addFavorite(principal.getName(), bookId);
        return ResponseEntity.ok(ApiResponse.success("Added to favorites", null));
    }

    @DeleteMapping("/books/{bookId}/favorite")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Void>> removeFavorite(
            @PathVariable Long bookId,
            Principal principal) {
        engagementService.removeFavorite(principal.getName(), bookId);
        return ResponseEntity.ok(ApiResponse.success("Removed from favorites", null));
    }

    @GetMapping("/users/me/favorites")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<BookResponse>>> getMyFavorites(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Principal principal) {
        PageResponse<BookResponse> favorites = engagementService.getMyFavorites(principal.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Favorites retrieved", favorites));
    }

    @GetMapping("/books/{bookId}/favorite/status")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<Boolean>> isFavorite(
            @PathVariable Long bookId,
            Principal principal) {
        boolean status = engagementService.isFavorite(principal.getName(), bookId);
        return ResponseEntity.ok(ApiResponse.success("Favorite status", status));
    }

    // ─── READING HISTORY ───────────────────────────────────────────────────────

    @PostMapping("/books/{bookId}/progress")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<ReadingHistoryResponse>> updateProgress(
            @PathVariable Long bookId,
            @RequestParam int lastPage,
            @RequestParam BigDecimal progress,
            Principal principal) {
        ReadingHistoryResponse rh = engagementService.updateReadingProgress(
                principal.getName(), bookId, lastPage, progress);
        return ResponseEntity.ok(ApiResponse.success("Reading progress updated", rh));
    }

    @GetMapping("/users/me/reading-history")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<PageResponse<ReadingHistoryResponse>>> getMyReadingHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {
        PageResponse<ReadingHistoryResponse> history = engagementService.getMyReadingHistory(
                principal.getName(), page, size);
        return ResponseEntity.ok(ApiResponse.success("Reading history retrieved", history));
    }

    // ─── RECOMMENDATIONS ────────────────────────────────────────────────────────

    @GetMapping("/books/recommendations")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<BookResponse>>> getRecommendations(
            @RequestParam(defaultValue = "10") int limit,
            Principal principal) {
        List<BookResponse> recommendations = engagementService.getRecommendations(principal.getName(), limit);
        return ResponseEntity.ok(ApiResponse.success("Recommendations retrieved", recommendations));
    }
}
