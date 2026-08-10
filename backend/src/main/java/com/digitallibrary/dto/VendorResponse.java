package com.digitallibrary.dto;

import com.digitallibrary.entity.VendorProfile;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class VendorResponse {
    private Long id;
    private Long userId;
    private String userEmail;
    private String storeName;
    private String bio;
    private BigDecimal commissionRate;
    private String status;
    private LocalDateTime approvedAt;
    private LocalDateTime createdAt;

    public VendorResponse() {}

    public static VendorResponse fromEntity(VendorProfile profile) {
        VendorResponse response = new VendorResponse();
        response.setId(profile.getId());
        if (profile.getUser() != null) {
            response.setUserId(profile.getUser().getId());
            response.setUserEmail(profile.getUser().getEmail());
        }
        response.setStoreName(profile.getStoreName());
        response.setBio(profile.getBio());
        response.setCommissionRate(profile.getCommissionRate());
        response.setStatus(profile.getStatus());
        response.setApprovedAt(profile.getApprovedAt());
        response.setCreatedAt(profile.getCreatedAt());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getApprovedAt() { return approvedAt; }
    public void setApprovedAt(LocalDateTime approvedAt) { this.approvedAt = approvedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
