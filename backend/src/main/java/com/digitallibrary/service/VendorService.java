package com.digitallibrary.service;

import com.digitallibrary.dto.PageResponse;
import com.digitallibrary.dto.VendorApplicationRequest;
import com.digitallibrary.dto.VendorResponse;

import java.math.BigDecimal;

public interface VendorService {
    VendorResponse applyForVendor(String userEmail, VendorApplicationRequest request);
    VendorResponse getVendorProfile(String userEmail);
    PageResponse<VendorResponse> getPendingApplications(int page, int size);
    VendorResponse approveVendor(Long vendorId, BigDecimal commissionRate);
    VendorResponse rejectVendor(Long vendorId, String reason);
    PageResponse<com.digitallibrary.dto.BookResponse> getVendorBooks(String userEmail, int page, int size);
    PageResponse<com.digitallibrary.dto.CommissionResponse> getVendorCommissions(String userEmail, int page, int size);
}
