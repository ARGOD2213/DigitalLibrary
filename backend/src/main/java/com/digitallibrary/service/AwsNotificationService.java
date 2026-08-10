package com.digitallibrary.service;

public interface AwsNotificationService {
    void sendEmail(String toEmail, String subject, String bodyHtml);
    void sendSms(String phoneNumber, String message);
}
