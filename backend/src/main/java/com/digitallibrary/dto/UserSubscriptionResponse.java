package com.digitallibrary.dto;

import com.digitallibrary.entity.UserSubscription;
import java.time.LocalDateTime;

public class UserSubscriptionResponse {
    private Long id;
    private Long userId;
    private SubscriptionPlanResponse plan;
    private String status;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private boolean autoRenew;
    private LocalDateTime createdAt;

    public UserSubscriptionResponse() {}

    public static UserSubscriptionResponse fromEntity(UserSubscription subscription) {
        UserSubscriptionResponse response = new UserSubscriptionResponse();
        response.setId(subscription.getId());
        if (subscription.getUser() != null) {
            response.setUserId(subscription.getUser().getId());
        }
        if (subscription.getPlan() != null) {
            response.setPlan(SubscriptionPlanResponse.fromEntity(subscription.getPlan()));
        }
        response.setStatus(subscription.getStatus());
        response.setStartDate(subscription.getStartDate());
        response.setEndDate(subscription.getEndDate());
        response.setAutoRenew(subscription.isAutoRenew());
        response.setCreatedAt(subscription.getCreatedAt());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public SubscriptionPlanResponse getPlan() { return plan; }
    public void setPlan(SubscriptionPlanResponse plan) { this.plan = plan; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getStartDate() { return startDate; }
    public void setStartDate(LocalDateTime startDate) { this.startDate = startDate; }
    public LocalDateTime getEndDate() { return endDate; }
    public void setEndDate(LocalDateTime endDate) { this.endDate = endDate; }
    public boolean isAutoRenew() { return autoRenew; }
    public void setAutoRenew(boolean autoRenew) { this.autoRenew = autoRenew; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
