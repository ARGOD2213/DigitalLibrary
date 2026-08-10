package com.digitallibrary.service;

import com.digitallibrary.dto.CreateSubscriptionPlanRequest;
import com.digitallibrary.dto.SubscriptionPlanResponse;
import com.digitallibrary.dto.UserSubscriptionResponse;

import java.util.List;

public interface SubscriptionService {
    List<SubscriptionPlanResponse> getActivePlans();
    SubscriptionPlanResponse createPlan(CreateSubscriptionPlanRequest request);
    UserSubscriptionResponse subscribeUser(String userEmail, Long planId);
    UserSubscriptionResponse getCurrentSubscription(String userEmail);
    void processExpiredSubscriptions();
}
