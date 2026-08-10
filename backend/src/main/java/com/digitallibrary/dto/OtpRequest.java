package com.digitallibrary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class OtpRequest {

    @NotBlank(message = "Target (email or phone) is required")
    private String target;

    @NotBlank(message = "OTP type is required")
    private String type; // REGISTRATION, PASSWORD_RESET, LOGIN

    @NotNull(message = "Channel is required")
    private String channel; // EMAIL, SMS

    public OtpRequest() {}

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
}
