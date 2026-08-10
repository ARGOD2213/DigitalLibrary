package com.digitallibrary.service;

import com.digitallibrary.entity.AppUser;

public interface OtpService {
    void generateAndSendOtp(AppUser user, String target, String type, String channel);
    boolean verifyOtp(String target, String type, String code);
    void resendOtp(AppUser user, String target, String type, String channel);
}
