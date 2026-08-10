package com.digitallibrary.controller;

import com.digitallibrary.dto.ApiResponse;
import com.digitallibrary.dto.OtpRequest;
import com.digitallibrary.dto.OtpVerifyRequest;
import com.digitallibrary.entity.AppUser;
import com.digitallibrary.exception.ResourceNotFoundException;
import com.digitallibrary.repository.AppUserRepository;
import com.digitallibrary.service.OtpService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/otps")
public class OtpController {

    private final OtpService otpService;
    private final AppUserRepository appUserRepository;

    public OtpController(OtpService otpService, AppUserRepository appUserRepository) {
        this.otpService = otpService;
        this.appUserRepository = appUserRepository;
    }

    @PostMapping("/send")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> sendOtp(@Valid @RequestBody OtpRequest request,
                                       Authentication authentication) {
        String email = authentication.getName();
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        otpService.generateAndSendOtp(user, request.getTarget(), request.getType(), request.getChannel());
        return ApiResponse.success("OTP sent successfully", "OTP sent to " + request.getTarget());
    }

    @PostMapping("/verify")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> verifyOtp(@Valid @RequestBody OtpVerifyRequest request,
                                         Authentication authentication) {
        otpService.verifyOtp(request.getTarget(), request.getType(), request.getCode());
        return ApiResponse.success("OTP verified successfully", "Verification complete");
    }

    @PostMapping("/resend")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> resendOtp(@Valid @RequestBody OtpRequest request,
                                         Authentication authentication) {
        String email = authentication.getName();
        AppUser user = appUserRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        otpService.resendOtp(user, request.getTarget(), request.getType(), request.getChannel());
        return ApiResponse.success("OTP resent successfully", "New OTP sent to " + request.getTarget());
    }
}
