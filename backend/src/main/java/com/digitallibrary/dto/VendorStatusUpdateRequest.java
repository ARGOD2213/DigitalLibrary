package com.digitallibrary.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;

public class VendorStatusUpdateRequest {

    @NotBlank(message = "Status is required")
    private String status; // APPROVED, REJECTED

    private String rejectionReason;

    private BigDecimal commissionRate; // Admin can specify at approval

    public VendorStatusUpdateRequest() {}

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }

    public BigDecimal getCommissionRate() { return commissionRate; }
    public void setCommissionRate(BigDecimal commissionRate) { this.commissionRate = commissionRate; }
}
