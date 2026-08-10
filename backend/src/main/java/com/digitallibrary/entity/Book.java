package com.digitallibrary.entity;

import com.digitallibrary.enums.AccessType;
import com.digitallibrary.enums.ContentType;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "books")
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Column(nullable = false)
    private String title;

    @Column
    private String subtitle;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    private Author authorEntity;

    @Column(name = "author_name")
    private String authorName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category categoryEntity;

    @Column(name = "category_name")
    private String categoryName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendor_profile_id")
    private VendorProfile vendorProfile;

    @Column(length = 50)
    private String isbn;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Min(value = 0, message = "Available copies must be zero or positive")
    @Column(name = "available_copies", nullable = false)
    private int availableCopies = 100;

    @Column(name = "is_free", nullable = false)
    private boolean free = false;

    @Column(name = "is_published", nullable = false)
    private boolean published = true;

    @Column(nullable = false, length = 50)
    private String status = "PUBLISHED";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String previewText;

    @Column
    private String publisher;

    @Column
    private String fileName;

    @Column
    private Long uploadedByUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type")
    private ContentType contentType = ContentType.BOOK;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_type")
    private AccessType accessType = AccessType.FREE;

    @Column
    private String tags;

    @Column(name = "cover_image_url", length = 512)
    private String coverImageUrl;

    @Column(name = "sample_file_url", length = 512)
    private String sampleFileUrl;

    @Column(name = "full_file_url", length = 512)
    private String fullFileUrl;

    @Column(name = "total_sales", nullable = false)
    private int totalSales = 0;

    @Column(name = "average_rating", nullable = false, precision = 3, scale = 2)
    private BigDecimal averageRating = BigDecimal.ZERO;

    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    public Book() {
    }

    public Book(String title, String authorName, String categoryName, String isbn, int availableCopies) {
        this.title = title;
        this.authorName = authorName;
        this.categoryName = categoryName;
        this.isbn = isbn;
        this.availableCopies = availableCopies;
    }

    @PrePersist
    public void beforeInsert() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void beforeUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public Author getAuthorEntity() { return authorEntity; }
    public void setAuthorEntity(Author authorEntity) { this.authorEntity = authorEntity; }

    public String getAuthorName() {
        if (authorName != null && !authorName.isBlank()) return authorName;
        if (authorEntity != null) return authorEntity.getName();
        return "";
    }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public String getAuthor() { return getAuthorName(); }
    public void setAuthor(String author) { setAuthorName(author); }

    public Category getCategoryEntity() { return categoryEntity; }
    public void setCategoryEntity(Category categoryEntity) { this.categoryEntity = categoryEntity; }

    public String getCategoryName() {
        if (categoryName != null && !categoryName.isBlank()) return categoryName;
        if (categoryEntity != null) return categoryEntity.getName();
        return "";
    }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getCategory() { return getCategoryName(); }
    public void setCategory(String category) { setCategoryName(category); }

    public VendorProfile getVendorProfile() { return vendorProfile; }
    public void setVendorProfile(VendorProfile vendorProfile) { this.vendorProfile = vendorProfile; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }

    public boolean isFree() { return free; }
    public void setFree(boolean free) { this.free = free; }

    public boolean isPublished() { return published; }
    public void setPublished(boolean published) { this.published = published; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPreviewText() { return previewText; }
    public void setPreviewText(String previewText) { this.previewText = previewText; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public Long getUploadedByUserId() { return uploadedByUserId; }
    public void setUploadedByUserId(Long uploadedByUserId) { this.uploadedByUserId = uploadedByUserId; }

    public ContentType getContentType() { return contentType; }
    public void setContentType(ContentType contentType) { this.contentType = contentType; }

    public AccessType getAccessType() { return accessType; }
    public void setAccessType(AccessType accessType) { this.accessType = accessType; }

    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }

    public String getCoverImageUrl() { return coverImageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }

    public String getSampleFileUrl() { return sampleFileUrl; }
    public void setSampleFileUrl(String sampleFileUrl) { this.sampleFileUrl = sampleFileUrl; }

    public String getFullFileUrl() { return fullFileUrl; }
    public void setFullFileUrl(String fullFileUrl) { this.fullFileUrl = fullFileUrl; }

    public String getFileUrl() { return fullFileUrl != null ? fullFileUrl : sampleFileUrl; }
    public void setFileUrl(String url) { this.fullFileUrl = url; }

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

    public LocalDateTime getDeletedAt() { return deletedAt; }
    public void setDeletedAt(LocalDateTime deletedAt) { this.deletedAt = deletedAt; }
}
