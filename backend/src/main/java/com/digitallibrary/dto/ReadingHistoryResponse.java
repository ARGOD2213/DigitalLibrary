package com.digitallibrary.dto;

import com.digitallibrary.entity.ReadingHistory;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ReadingHistoryResponse {
    private Long id;
    private Long bookId;
    private String bookTitle;
    private String coverImageUrl;
    private int lastPageRead;
    private BigDecimal progressPercentage;
    private LocalDateTime lastReadAt;

    public ReadingHistoryResponse() {}

    public static ReadingHistoryResponse fromEntity(ReadingHistory rh) {
        ReadingHistoryResponse r = new ReadingHistoryResponse();
        r.setId(rh.getId());
        if (rh.getBook() != null) {
            r.setBookId(rh.getBook().getId());
            r.setBookTitle(rh.getBook().getTitle());
            r.setCoverImageUrl(rh.getBook().getCoverImageUrl());
        }
        r.setLastPageRead(rh.getLastPageRead());
        r.setProgressPercentage(rh.getProgressPercentage());
        r.setLastReadAt(rh.getLastReadAt());
        return r;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public int getLastPageRead() { return lastPageRead; }
    public void setLastPageRead(int lastPageRead) { this.lastPageRead = lastPageRead; }
    public BigDecimal getProgressPercentage() { return progressPercentage; }
    public void setProgressPercentage(BigDecimal progressPercentage) { this.progressPercentage = progressPercentage; }
    public LocalDateTime getLastReadAt() { return lastReadAt; }
    public void setLastReadAt(LocalDateTime lastReadAt) { this.lastReadAt = lastReadAt; }
}
