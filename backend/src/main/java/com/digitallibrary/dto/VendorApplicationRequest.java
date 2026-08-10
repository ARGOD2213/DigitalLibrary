package com.digitallibrary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class VendorApplicationRequest {

    @NotBlank(message = "Store name is required")
    @Size(max = 150, message = "Store name cannot exceed 150 characters")
    private String storeName;

    private String bio;

    public VendorApplicationRequest() {}

    public String getStoreName() { return storeName; }
    public void setStoreName(String storeName) { this.storeName = storeName; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
}
