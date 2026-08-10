package com.digitallibrary.service.impl;

import com.digitallibrary.entity.AppUser;
import com.digitallibrary.entity.Otp;
import com.digitallibrary.exception.AuthenticationException;
import com.digitallibrary.repository.OtpRepository;
import com.digitallibrary.service.AwsNotificationService;
import com.digitallibrary.service.OtpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OtpServiceImpl implements OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpServiceImpl.class);

    private static final int OTP_LENGTH = 6;
    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 3;
    private static final int RATE_LIMIT_PER_HOUR = 5;

    private final OtpRepository otpRepository;
    private final AwsNotificationService notificationService;

    @Value("${aws.mock-enabled:true}")
    private boolean mockEnabled;

    public OtpServiceImpl(OtpRepository otpRepository, AwsNotificationService notificationService) {
        this.otpRepository = otpRepository;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public void generateAndSendOtp(AppUser user, String target, String type, String channel) {
        // Rate limiting: max 5 OTPs per target per hour
        long recentCount = otpRepository.countRecentOtps(target, type, LocalDateTime.now().minusHours(1));
        if (recentCount >= RATE_LIMIT_PER_HOUR) {
            throw new AuthenticationException("Too many OTP requests. Please try again later.");
        }

        // Expire all existing active OTPs for this target+type
        List<Otp> existingOtps = otpRepository.findValidOtps(target, type, LocalDateTime.now());
        existingOtps.forEach(otp -> otp.setUsed(true));
        otpRepository.saveAll(existingOtps);

        // Generate new OTP
        String code = generateOtpCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        Otp otp = new Otp(user, target, code, type, channel, expiresAt);
        otpRepository.save(otp);

        // Send via appropriate channel
        if ("EMAIL".equalsIgnoreCase(channel)) {
            sendEmailOtp(target, code, type);
        } else if ("SMS".equalsIgnoreCase(channel)) {
            sendSmsOtp(target, code, type);
        }

        log.info("OTP generated and sent for target={} type={} channel={}", target, type, channel);
    }

    @Override
    @Transactional
    public boolean verifyOtp(String target, String type, String code) {
        List<Otp> validOtps = otpRepository.findValidOtps(target, type, LocalDateTime.now());

        if (validOtps.isEmpty()) {
            throw new AuthenticationException("OTP has expired or does not exist. Please request a new one.");
        }

        Otp latestOtp = validOtps.get(0);

        // Check max attempts
        if (latestOtp.getAttemptCount() >= MAX_ATTEMPTS) {
            latestOtp.setUsed(true);
            otpRepository.save(latestOtp);
            throw new AuthenticationException("Maximum OTP attempts exceeded. Please request a new OTP.");
        }

        // Increment attempt count
        latestOtp.setAttemptCount(latestOtp.getAttemptCount() + 1);

        if (!latestOtp.getCode().equals(code)) {
            otpRepository.save(latestOtp);
            int remainingAttempts = MAX_ATTEMPTS - latestOtp.getAttemptCount();
            throw new AuthenticationException("Invalid OTP. " + remainingAttempts + " attempt(s) remaining.");
        }

        // OTP is valid
        latestOtp.setUsed(true);
        otpRepository.save(latestOtp);
        log.info("OTP verified successfully for target={} type={}", target, type);
        return true;
    }

    @Override
    @Transactional
    public void resendOtp(AppUser user, String target, String type, String channel) {
        generateAndSendOtp(user, target, type, channel);
    }

    private String generateOtpCode() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

    private void sendEmailOtp(String email, String code, String type) {
        String subject = getEmailSubject(type);
        String body = buildEmailBody(code, type);
        notificationService.sendEmail(email, subject, body);
    }

    private void sendSmsOtp(String phone, String code, String type) {
        String message = "Your Digital Library " + type.replace("_", " ").toLowerCase() +
                " OTP is: " + code + ". Valid for " + OTP_EXPIRY_MINUTES + " minutes. Do not share.";
        notificationService.sendSms(phone, message);
    }

    private String getEmailSubject(String type) {
        return switch (type.toUpperCase()) {
            case "REGISTRATION" -> "Verify Your Email - Digital Library";
            case "PASSWORD_RESET" -> "Password Reset OTP - Digital Library";
            case "LOGIN" -> "Login Verification OTP - Digital Library";
            default -> "Your OTP - Digital Library";
        };
    }

    private String buildEmailBody(String code, String type) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 30px;">
                  <div style="background: linear-gradient(135deg, #667eea 0%%, #764ba2 100%%); padding: 30px; border-radius: 10px 10px 0 0; text-align: center;">
                    <h1 style="color: white; margin: 0;">Digital Library</h1>
                  </div>
                  <div style="background: #f9f9f9; padding: 30px; border-radius: 0 0 10px 10px; border: 1px solid #eee;">
                    <h2 style="color: #333;">Your Verification Code</h2>
                    <p style="color: #666;">Use the OTP below to complete your %s:</p>
                    <div style="background: white; border: 2px solid #667eea; border-radius: 8px; padding: 20px; text-align: center; margin: 20px 0;">
                      <h1 style="color: #667eea; font-size: 36px; letter-spacing: 8px; margin: 0;">%s</h1>
                    </div>
                    <p style="color: #666;">This OTP expires in <strong>%d minutes</strong>. Do not share it with anyone.</p>
                    <p style="color: #999; font-size: 12px;">If you did not request this, please ignore this email.</p>
                  </div>
                </body>
                </html>
                """.formatted(type.replace("_", " ").toLowerCase(), code, OTP_EXPIRY_MINUTES);
    }
}
