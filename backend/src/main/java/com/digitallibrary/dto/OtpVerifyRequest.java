package com.digitallibrary.dto;

import jakarta.validation.constraints.NotBlank;

public class OtpVerifyRequest {

    @NotBlank(message = "Target is required")
    private String target;

    @NotBlank(message = "OTP type is required")
    private String type;

    @NotBlank(message = "OTP code is required")
    private String code;

    public OtpVerifyRequest() {}

    public String getTarget() { return target; }
    public void setTarget(String target) { this.target = target; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
