package com.digitallibrary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class CreateSubscriptionPlanRequest {
    
    @NotBlank(message = "Plan name is required")
    private String name;
    
    @NotBlank(message = "Plan type is required")
    private String planType; // FREE, MONTHLY, YEARLY
    
    @NotNull(message = "Price is required")
    private BigDecimal price;
    
    @NotNull(message = "Duration in days is required")
    private Integer durationDays;
    
    @NotNull(message = "Max downloads per month is required")
    private Integer maxDownloadsPerMonth;
    
    private String featuresJson;

    public CreateSubscriptionPlanRequest() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPlanType() { return planType; }
    public void setPlanType(String planType) { this.planType = planType; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Integer getDurationDays() { return durationDays; }
    public void setDurationDays(Integer durationDays) { this.durationDays = durationDays; }

    public Integer getMaxDownloadsPerMonth() { return maxDownloadsPerMonth; }
    public void setMaxDownloadsPerMonth(Integer maxDownloadsPerMonth) { this.maxDownloadsPerMonth = maxDownloadsPerMonth; }

    public String getFeaturesJson() { return featuresJson; }
    public void setFeaturesJson(String featuresJson) { this.featuresJson = featuresJson; }
}
