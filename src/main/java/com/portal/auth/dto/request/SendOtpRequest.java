package com.portal.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public class SendOtpRequest {
    @NotBlank
    private String account;

    @NotBlank
    private String captchaToken;

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public String getCaptchaToken() { return captchaToken; }
    public void setCaptchaToken(String captchaToken) { this.captchaToken = captchaToken; }
}
