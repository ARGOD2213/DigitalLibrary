package com.digitallibrary.service.impl;

import com.digitallibrary.dto.PageResponse;
import com.digitallibrary.dto.VendorApplicationRequest;
import com.digitallibrary.dto.VendorResponse;
import com.digitallibrary.entity.AppUser;
import com.digitallibrary.entity.VendorProfile;
import com.digitallibrary.exception.DuplicateResourceException;
import com.digitallibrary.exception.ResourceNotFoundException;
import com.digitallibrary.repository.AppUserRepository;
import com.digitallibrary.repository.VendorProfileRepository;
import com.digitallibrary.service.AwsNotificationService;
import com.digitallibrary.service.VendorService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.digitallibrary.dto.BookResponse;
import com.digitallibrary.dto.CommissionResponse;
import com.digitallibrary.repository.BookRepository;
import com.digitallibrary.repository.CommissionRepository;
import org.springframework.data.domain.Pageable;

@Service
public class VendorServiceImpl implements VendorService {

    private final VendorProfileRepository vendorProfileRepository;
    private final AppUserRepository appUserRepository;
    private final AwsNotificationService awsNotificationService;
    private final BookRepository bookRepository;
    private final CommissionRepository commissionRepository;

    public VendorServiceImpl(VendorProfileRepository vendorProfileRepository,
                             AppUserRepository appUserRepository,
                             AwsNotificationService awsNotificationService,
                             BookRepository bookRepository,
                             CommissionRepository commissionRepository) {
        this.vendorProfileRepository = vendorProfileRepository;
        this.appUserRepository = appUserRepository;
        this.awsNotificationService = awsNotificationService;
        this.bookRepository = bookRepository;
        this.commissionRepository = commissionRepository;
    }

    @Override
    @Transactional
    public VendorResponse applyForVendor(String userEmail, VendorApplicationRequest request) {
        AppUser user = appUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (vendorProfileRepository.existsByUserId(user.getId())) {
            throw new DuplicateResourceException("You have already submitted a vendor application");
        }

        VendorProfile profile = new VendorProfile(user, request.getStoreName(), request.getBio());
        VendorProfile savedProfile = vendorProfileRepository.save(profile);
        return VendorResponse.fromEntity(savedProfile);
    }

    @Override
    public VendorResponse getVendorProfile(String userEmail) {
        VendorProfile profile = vendorProfileRepository.findByUserEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor profile not found for this user"));
        return VendorResponse.fromEntity(profile);
    }

    @Override
    public PageResponse<VendorResponse> getPendingApplications(int page, int size) {
        Page<VendorProfile> profiles = vendorProfileRepository.findByStatus("PENDING", PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt")));
        return PageResponse.fromPage(profiles.map(VendorResponse::fromEntity));
    }

    @Override
    @Transactional
    public VendorResponse approveVendor(Long vendorId, BigDecimal commissionRate) {
        VendorProfile profile = vendorProfileRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor application not found"));

        profile.setStatus("APPROVED");
        profile.setApprovedAt(LocalDateTime.now());
        if (commissionRate != null) {
            profile.setCommissionRate(commissionRate);
        }

        AppUser user = profile.getUser();
        if (user.getRole() != com.digitallibrary.enums.UserRole.ROLE_VENDOR) {
            user.setRole(com.digitallibrary.enums.UserRole.ROLE_VENDOR);
            appUserRepository.save(user);
        }

        VendorProfile savedProfile = vendorProfileRepository.save(profile);

        // Send Notification
        String subject = "Vendor Application Approved - Digital Library";
        String body = "Congratulations! Your vendor application for '" + profile.getStoreName() + "' has been approved.";
        awsNotificationService.sendEmail(user.getEmail(), subject, body);

        return VendorResponse.fromEntity(savedProfile);
    }

    @Override
    @Transactional
    public VendorResponse rejectVendor(Long vendorId, String reason) {
        VendorProfile profile = vendorProfileRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor application not found"));

        profile.setStatus("REJECTED");
        // We could store rejection reason in a new column if needed, or just send it in the email
        VendorProfile savedProfile = vendorProfileRepository.save(profile);

        AppUser user = profile.getUser();
        
        // Send Notification
        String subject = "Vendor Application Update - Digital Library";
        String body = "We regret to inform you that your vendor application for '" + profile.getStoreName() + "' has been rejected. Reason: " + (reason != null ? reason : "Not specified");
        awsNotificationService.sendEmail(user.getEmail(), subject, body);

        return VendorResponse.fromEntity(savedProfile);
    }

    @Override
    public PageResponse<BookResponse> getVendorBooks(String userEmail, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<com.digitallibrary.entity.Book> books = bookRepository.findByVendorProfileUserEmailAndDeletedAtIsNull(userEmail, pageable);
        return PageResponse.fromPage(books.map(BookResponse::fromEntity));
    }

    @Override
    public PageResponse<CommissionResponse> getVendorCommissions(String userEmail, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<com.digitallibrary.entity.Commission> commissions = commissionRepository.findByVendorProfileUserEmail(userEmail, pageable);
        return PageResponse.fromPage(commissions.map(CommissionResponse::fromEntity));
    }
}
