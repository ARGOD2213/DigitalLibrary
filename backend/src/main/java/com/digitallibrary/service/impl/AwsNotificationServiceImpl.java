package com.digitallibrary.service.impl;

import com.digitallibrary.service.AwsNotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ses.SesClient;
import software.amazon.awssdk.services.ses.model.*;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;
import software.amazon.awssdk.services.sns.model.PublishResponse;

@Service
public class AwsNotificationServiceImpl implements AwsNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AwsNotificationServiceImpl.class);

    private final SesClient sesClient;
    private final SnsClient snsClient;

    @Value("${aws.ses.from-email:noreply@digitallibrary.com}")
    private String fromEmail;

    @Value("${aws.sns.sender-id:DIGILIB}")
    private String senderId;

    @Value("${aws.mock-enabled:true}")
    private boolean mockEnabled;

    public AwsNotificationServiceImpl(SesClient sesClient, SnsClient snsClient) {
        this.sesClient = sesClient;
        this.snsClient = snsClient;
    }

    @Override
    public void sendEmail(String toEmail, String subject, String bodyHtml) {
        if (toEmail == null || toEmail.isBlank()) return;

        if (mockEnabled) {
            log.info("[MOCK AWS SES EMAIL] To: {} | Subject: {} | Body: {}", toEmail, subject, bodyHtml);
            return;
        }

        try {
            SendEmailRequest emailRequest = SendEmailRequest.builder()
                    .destination(Destination.builder().toAddresses(toEmail).build())
                    .message(Message.builder()
                            .subject(Content.builder().data(subject).build())
                            .body(Body.builder().html(Content.builder().data(bodyHtml).build()).build())
                            .build())
                    .source(fromEmail)
                    .build();

            SendEmailResponse response = sesClient.sendEmail(emailRequest);
            log.info("Successfully sent AWS SES Email to {} (MessageId: {})", toEmail, response.messageId());
        } catch (Exception e) {
            log.error("Failed to send AWS SES Email to {}: {}", toEmail, e.getMessage(), e);
            log.info("[FALLBACK EMAIL LOG] To: {} | Subject: {} | Body: {}", toEmail, subject, bodyHtml);
        }
    }

    @Override
    public void sendSms(String phoneNumber, String message) {
        if (phoneNumber == null || phoneNumber.isBlank()) return;

        if (mockEnabled) {
            log.info("[MOCK AWS SNS SMS] Phone: {} | Message: {}", phoneNumber, message);
            return;
        }

        try {
            PublishRequest publishRequest = PublishRequest.builder()
                    .message(message)
                    .phoneNumber(phoneNumber)
                    .build();

            PublishResponse response = snsClient.publish(publishRequest);
            log.info("Successfully sent AWS SNS SMS to {} (MessageId: {})", phoneNumber, response.messageId());
        } catch (Exception e) {
            log.error("Failed to send AWS SNS SMS to {}: {}", phoneNumber, e.getMessage(), e);
            log.info("[FALLBACK SMS LOG] Phone: {} | Message: {}", phoneNumber, message);
        }
    }
}
