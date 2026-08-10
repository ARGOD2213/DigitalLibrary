package com.digitallibrary.dto;

import com.digitallibrary.entity.Review;
import java.time.LocalDateTime;

public class ReviewResponse {
    private Long id;
    private Long userId;
    private String userName;
    private Long bookId;
    private String bookTitle;
    private int rating;
    private String comment;
    private boolean moderated;
    private LocalDateTime createdAt;

    public ReviewResponse() {}

    public static ReviewResponse fromEntity(Review review) {
        ReviewResponse r = new ReviewResponse();
        r.setId(review.getId());
        if (review.getUser() != null) {
            r.setUserId(review.getUser().getId());
            r.setUserName(review.getUser().getFullName());
        }
        if (review.getBook() != null) {
            r.setBookId(review.getBook().getId());
            r.setBookTitle(review.getBook().getTitle());
        }
        r.setRating(review.getRating());
        r.setComment(review.getComment());
        r.setModerated(review.isModerated());
        r.setCreatedAt(review.getCreatedAt());
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    public int getRating() { return rating; }
    public void setRating(int rating) { this.rating = rating; }
    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }
    public boolean isModerated() { return moderated; }
    public void setModerated(boolean moderated) { this.moderated = moderated; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
