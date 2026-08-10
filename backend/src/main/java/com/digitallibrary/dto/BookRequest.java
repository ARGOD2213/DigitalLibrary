package com.digitallibrary.dto;

import com.digitallibrary.enums.AccessType;
import com.digitallibrary.enums.ContentType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public class BookRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String subtitle;

    @NotBlank(message = "Author is required")
    private String author;

    @NotBlank(message = "Category is required")
    private String category;

    private String isbn;

    @Min(value = 0, message = "Available copies must be zero or positive")
    private int availableCopies = 100;

    private BigDecimal price = BigDecimal.ZERO;

    private boolean free = false;
    private boolean published = true;
    private String status = "PUBLISHED"; // PUBLISHED, DRAFT, ARCHIVED

    private ContentType contentType = ContentType.BOOK;
    private AccessType accessType = AccessType.FREE;

    private String description;
    private String previewText;
    private String publisher;
    private String tags;
    private String coverImageUrl;

    public BookRequest() {}

    // Getters and Setters
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
}
