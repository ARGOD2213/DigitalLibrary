package com.digitallibrary.controller;

import com.digitallibrary.dto.ApiResponse;
import com.digitallibrary.dto.CreateSubscriptionPlanRequest;
import com.digitallibrary.dto.SubscriptionPlanResponse;
import com.digitallibrary.dto.UserSubscriptionResponse;
import com.digitallibrary.service.SubscriptionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    public SubscriptionController(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/plans")
    public ResponseEntity<ApiResponse<List<SubscriptionPlanResponse>>> getActivePlans() {
        List<SubscriptionPlanResponse> plans = subscriptionService.getActivePlans();
        return ResponseEntity.ok(ApiResponse.success("Active subscription plans retrieved", plans));
    }

    @PostMapping("/plans")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SubscriptionPlanResponse>> createPlan(
            @Valid @RequestBody CreateSubscriptionPlanRequest request) {
        SubscriptionPlanResponse plan = subscriptionService.createPlan(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subscription plan created successfully", plan));
    }

    @PostMapping("/subscribe/{planId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserSubscriptionResponse>> subscribe(
            @PathVariable Long planId,
            Principal principal) {
        UserSubscriptionResponse subscription = subscriptionService.subscribeUser(principal.getName(), planId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subscribed successfully", subscription));
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<UserSubscriptionResponse>> getMySubscription(Principal principal) {
        UserSubscriptionResponse subscription = subscriptionService.getCurrentSubscription(principal.getName());
        if (subscription == null) {
            return ResponseEntity.ok(ApiResponse.success("No active subscription found", null));
        }
        return ResponseEntity.ok(ApiResponse.success("Current subscription retrieved", subscription));
    }
}
