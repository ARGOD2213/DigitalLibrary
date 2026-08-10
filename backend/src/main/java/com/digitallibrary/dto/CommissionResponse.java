package com.digitallibrary.dto;

import com.digitallibrary.entity.Commission;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CommissionResponse {
    private Long id;
    private Long orderItemId;
    private Long bookId;
    private String bookTitle;
    private BigDecimal grossAmount;
    private BigDecimal platformCommission;
    private BigDecimal vendorEarning;
    private String status;
    private LocalDateTime createdAt;

    public CommissionResponse() {}

    public static CommissionResponse fromEntity(Commission commission) {
        CommissionResponse response = new CommissionResponse();
        response.setId(commission.getId());
        if (commission.getOrderItem() != null) {
            response.setOrderItemId(commission.getOrderItem().getId());
            if (commission.getOrderItem().getBook() != null) {
                response.setBookId(commission.getOrderItem().getBook().getId());
                response.setBookTitle(commission.getOrderItem().getBook().getTitle());
            }
        }
        response.setGrossAmount(commission.getGrossAmount());
        response.setPlatformCommission(commission.getPlatformCommission());
        response.setVendorEarning(commission.getVendorEarning());
        response.setStatus(commission.getStatus());
        response.setCreatedAt(commission.getCreatedAt());
        return response;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderItemId() { return orderItemId; }
    public void setOrderItemId(Long orderItemId) { this.orderItemId = orderItemId; }
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }
    public BigDecimal getGrossAmount() { return grossAmount; }
    public void setGrossAmount(BigDecimal grossAmount) { this.grossAmount = grossAmount; }
    public BigDecimal getPlatformCommission() { return platformCommission; }
    public void setPlatformCommission(BigDecimal platformCommission) { this.platformCommission = platformCommission; }
    public BigDecimal getVendorEarning() { return vendorEarning; }
    public void setVendorEarning(BigDecimal vendorEarning) { this.vendorEarning = vendorEarning; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
