package com.digitallibrary.dto;

import com.digitallibrary.entity.SubscriptionPlan;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class SubscriptionPlanResponse {
    private Long id;
    private String name;
    private String planType;
    private BigDecimal price;
    private int durationDays;
    private int maxDownloadsPerMonth;
    private String featuresJson;
    private boolean active;
    private LocalDateTime createdAt;

    public SubscriptionPlanResponse() {}

    public static SubscriptionPlanResponse fromEntity(SubscriptionPlan plan) {
        SubscriptionPlanResponse response = new SubscriptionPlanResponse();
        response.setId(plan.getId());
        response.setName(plan.getName());
        response.setPlanType(plan.getPlanType());
        response.setPrice(plan.getPrice());
        response.setDurationDays(plan.getDurationDays());
        response.setMaxDownloadsPerMonth(plan.getMaxDownloadsPerMonth());
        response.setFeaturesJson(plan.getFeaturesJson());
        response.setActive(plan.isActive());
        response.setCreatedAt(plan.getCreatedAt());
        return response;
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
