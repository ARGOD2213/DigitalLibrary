package com.digitallibrary.scheduler;

import com.digitallibrary.service.SubscriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionExpiryScheduler {

    private static final Logger log = LoggerFactory.getLogger(SubscriptionExpiryScheduler.class);

    private final SubscriptionService subscriptionService;

    public SubscriptionExpiryScheduler(SubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    // Run daily at midnight server time
    @Scheduled(cron = "0 0 0 * * ?")
    public void processExpiredSubscriptions() {
        log.info("Starting daily expired subscription processing job...");
        try {
            subscriptionService.processExpiredSubscriptions();
            log.info("Finished expired subscription processing job successfully.");
        } catch (Exception e) {
            log.error("Error occurred while processing expired subscriptions", e);
        }
    }
}
