package com.digitallibrary.dto;

import com.digitallibrary.entity.Book;
import com.digitallibrary.enums.AccessType;
import com.digitallibrary.enums.ContentType;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class BookResponse {

    private Long id;
    private String title;
    private String subtitle;
    private String author;
    private String category;
    private String isbn;
    private int availableCopies;
    private BigDecimal price;
    private boolean free;
    private boolean published;
    private String status;
    private ContentType contentType;
    private AccessType accessType;
    private String description;
    private String previewText;
    private String publisher;
    private String tags;
    private String coverImageUrl;
    private String fileName;
    private String fileUrl;
    private Long uploadedByUserId;
    private int totalSales;
    private BigDecimal averageRating;
    private int viewCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public BookResponse() {}

    public static BookResponse fromEntity(Book book) {
        BookResponse response = new BookResponse();
        response.setId(book.getId());
        response.setTitle(book.getTitle());
        response.setSubtitle(book.getSubtitle());
        response.setAuthor(book.getAuthor());
        response.setCategory(book.getCategory());
        response.setIsbn(book.getIsbn());
        response.setAvailableCopies(book.getAvailableCopies());
        response.setPrice(book.getPrice());
        response.setFree(book.isFree());
        response.setPublished(book.isPublished());
        response.setStatus(book.getStatus());
        response.setContentType(book.getContentType());
        response.setAccessType(book.getAccessType());
        response.setDescription(book.getDescription());
        response.setPreviewText(book.getPreviewText());
        response.setPublisher(book.getPublisher());
        response.setTags(book.getTags());
        response.setCoverImageUrl(book.getCoverImageUrl());
        response.setFileName(book.getFileName());
        response.setFileUrl(book.getFileUrl());
        response.setUploadedByUserId(book.getUploadedByUserId());
        response.setTotalSales(book.getTotalSales());
        response.setAverageRating(book.getAverageRating());
        response.setViewCount(book.getViewCount());
        response.setCreatedAt(book.getCreatedAt());
        response.setUpdatedAt(book.getUpdatedAt());
        return response;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public boolean isFree() { return free; }
    public void setFree(boolean free) { this.free = free; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }

    public AccessType getAccessType() { return accessType; }
    public void setAccessType(AccessType accessType) { this.accessType = accessType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPreviewText() { return previewText; }
    public void setPreviewText(String previewText) { this.previewText = previewText; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public Long getUploadedByUserId() { return uploadedByUserId; }
    public void setUploadedByUserId(Long uploadedByUserId) { this.uploadedByUserId = uploadedByUserId; }

    public int getTotalSales() { return totalSales; }
    public void setTotalSales(int totalSales) { this.totalSales = totalSales; }

    public BigDecimal getAverageRating() { return averageRating; }
    public void setAverageRating(BigDecimal averageRating) { this.averageRating = averageRating; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
