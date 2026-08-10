package com.digitallibrary.controller;

import com.digitallibrary.dto.ApiResponse;
import com.digitallibrary.dto.PageResponse;
import com.digitallibrary.dto.VendorApplicationRequest;
import com.digitallibrary.dto.VendorResponse;
import com.digitallibrary.dto.VendorStatusUpdateRequest;
import com.digitallibrary.service.VendorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@RestController
@RequestMapping("/api/vendors")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @PostMapping("/apply")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<VendorResponse>> applyForVendor(
            @Valid @RequestBody VendorApplicationRequest request,
            Principal principal) {
        VendorResponse response = vendorService.applyForVendor(principal.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vendor application submitted successfully", response));
    }

    @GetMapping("/me")
    @PreAuthorize("hasRole('VENDOR') or isAuthenticated()")
    public ResponseEntity<ApiResponse<VendorResponse>> getMyVendorProfile(Principal principal) {
        VendorResponse response = vendorService.getVendorProfile(principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Vendor profile retrieved", response));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<VendorResponse>>> getPendingApplications(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<VendorResponse> pending = vendorService.getPendingApplications(page, size);
        return ResponseEntity.ok(ApiResponse.success("Pending applications retrieved", pending));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VendorResponse>> approveVendor(
            @PathVariable Long id,
            @RequestBody(required = false) VendorStatusUpdateRequest request) {
        
        VendorResponse response = vendorService.approveVendor(id, request != null ? request.getCommissionRate() : null);
        return ResponseEntity.ok(ApiResponse.success("Vendor approved successfully", response));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<VendorResponse>> rejectVendor(
            @PathVariable Long id,
            @RequestBody(required = false) VendorStatusUpdateRequest request) {
        
        VendorResponse response = vendorService.rejectVendor(id, request != null ? request.getRejectionReason() : null);
        return ResponseEntity.ok(ApiResponse.success("Vendor application rejected", response));
    }
}
