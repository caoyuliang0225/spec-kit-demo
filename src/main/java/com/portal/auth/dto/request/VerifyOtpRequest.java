package com.portal.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public class VerifyOtpRequest {
    @NotBlank
    private String account;

    @NotBlank
    private String otpCode;

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public String getOtpCode() { return otpCode; }
    public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
}
