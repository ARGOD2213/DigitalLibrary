package com.digitallibrary.service.impl;

import com.digitallibrary.dto.CreateSubscriptionPlanRequest;
import com.digitallibrary.dto.SubscriptionPlanResponse;
import com.digitallibrary.dto.UserSubscriptionResponse;
import com.digitallibrary.entity.AppUser;
import com.digitallibrary.entity.SubscriptionPlan;
import com.digitallibrary.entity.UserSubscription;
import com.digitallibrary.exception.ResourceNotFoundException;
import com.digitallibrary.repository.AppUserRepository;
import com.digitallibrary.repository.SubscriptionPlanRepository;
import com.digitallibrary.repository.UserSubscriptionRepository;
import com.digitallibrary.service.AwsNotificationService;
import com.digitallibrary.service.SubscriptionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class SubscriptionServiceImpl implements SubscriptionService {

    private final SubscriptionPlanRepository planRepository;
    private final UserSubscriptionRepository userSubscriptionRepository;
    private final AppUserRepository appUserRepository;
    private final AwsNotificationService awsNotificationService;

    public SubscriptionServiceImpl(SubscriptionPlanRepository planRepository,
                                   UserSubscriptionRepository userSubscriptionRepository,
                                   AppUserRepository appUserRepository,
                                   AwsNotificationService awsNotificationService) {
        this.planRepository = planRepository;
        this.userSubscriptionRepository = userSubscriptionRepository;
        this.appUserRepository = appUserRepository;
        this.awsNotificationService = awsNotificationService;
    }

    @Override
    public List<SubscriptionPlanResponse> getActivePlans() {
        return planRepository.findByActiveTrue().stream()
                .map(SubscriptionPlanResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SubscriptionPlanResponse createPlan(CreateSubscriptionPlanRequest request) {
        SubscriptionPlan plan = new SubscriptionPlan(
                request.getName(),
                request.getPlanType(),
                request.getPrice(),
                request.getDurationDays(),
                request.getMaxDownloadsPerMonth()
        );
        plan.setFeaturesJson(request.getFeaturesJson());
        SubscriptionPlan savedPlan = planRepository.save(plan);
        return SubscriptionPlanResponse.fromEntity(savedPlan);
    }

    @Override
    @Transactional
    public UserSubscriptionResponse subscribeUser(String userEmail, Long planId) {
        AppUser user = appUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SubscriptionPlan plan = planRepository.findById(planId)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription plan not found"));

        // Disable existing active subscriptions
        Optional<UserSubscription> existingActive = userSubscriptionRepository.findByUserIdAndStatus(user.getId(), "ACTIVE");
        existingActive.ifPresent(sub -> {
            sub.setStatus("CANCELLED");
            sub.setEndDate(LocalDateTime.now());
            userSubscriptionRepository.save(sub);
        });

        LocalDateTime startDate = LocalDateTime.now();
        LocalDateTime endDate = startDate.plusDays(plan.getDurationDays());

        UserSubscription subscription = new UserSubscription(user, plan, startDate, endDate);
        // Normally handled by payment gateway callback (Task 5.2), but we simulate success here
        subscription.setStatus("ACTIVE");
        
        UserSubscription savedSubscription = userSubscriptionRepository.save(subscription);

        // Send Notification
        String subject = "Subscription Confirmed - Digital Library";
        String body = "You have successfully subscribed to the '" + plan.getName() + "' plan. It is valid until " + endDate.toString() + ".";
        awsNotificationService.sendEmail(user.getEmail(), subject, body);

        return UserSubscriptionResponse.fromEntity(savedSubscription);
    }

    @Override
    public UserSubscriptionResponse getCurrentSubscription(String userEmail) {
        AppUser user = appUserRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UserSubscription activeSub = userSubscriptionRepository.findByUserIdAndStatus(user.getId(), "ACTIVE")
                .orElse(null);

        if (activeSub != null) {
            return UserSubscriptionResponse.fromEntity(activeSub);
        }

        List<UserSubscription> allSubs = userSubscriptionRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (!allSubs.isEmpty()) {
            return UserSubscriptionResponse.fromEntity(allSubs.get(0));
        }

        return null;
    }

    @Override
    @Transactional
    public void processExpiredSubscriptions() {
        LocalDateTime now = LocalDateTime.now();
        List<UserSubscription> expiredSubs = userSubscriptionRepository.findExpiredSubscriptions(now);

        for (UserSubscription sub : expiredSubs) {
            sub.setStatus("EXPIRED");
            userSubscriptionRepository.save(sub);

            // Send notification
            String subject = "Subscription Expired - Digital Library";
            String body = "Your '" + sub.getPlan().getName() + "' subscription has expired on " + sub.getEndDate().toString() + ". Please renew to continue enjoying premium features.";
            awsNotificationService.sendEmail(sub.getUser().getEmail(), subject, body);
        }
    }
}
