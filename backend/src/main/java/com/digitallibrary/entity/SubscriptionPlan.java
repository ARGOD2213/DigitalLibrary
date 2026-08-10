package com.digitallibrary.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_plans")
public class SubscriptionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "plan_type", nullable = false, length = 50)
    private String planType; // FREE, MONTHLY, YEARLY, PREMIUM

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    @Column(name = "duration_days", nullable = false)
    private int durationDays = 30;

    @Column(name = "max_downloads_per_month", nullable = false)
    private int maxDownloadsPerMonth = 10;

    @Column(name = "features_json", columnDefinition = "TEXT")
    private String featuresJson;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public SubscriptionPlan() {
    }

    public SubscriptionPlan(String name, String planType, BigDecimal price, int durationDays, int maxDownloadsPerMonth) {
        this.name = name;
        this.planType = planType;
        this.price = price;
        this.durationDays = durationDays;
        this.maxDownloadsPerMonth = maxDownloadsPerMonth;
    }

    @PrePersist
    public void beforeInsert() {
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public int getDurationDays() { return durationDays; }
    public void setDurationDays(int durationDays) { this.durationDays = durationDays; }

    public int getMaxDownloadsPerMonth() { return maxDownloadsPerMonth; }
    public void setMaxDownloadsPerMonth(int maxDownloadsPerMonth) { this.maxDownloadsPerMonth = maxDownloadsPerMonth; }

    public String getFeaturesJson() { return featuresJson; }
    public void setFeaturesJson(String featuresJson) { this.featuresJson = featuresJson; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
