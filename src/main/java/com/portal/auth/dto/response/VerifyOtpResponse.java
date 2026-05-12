package com.portal.auth.dto.response;

public class VerifyOtpResponse {
    private String tempToken;
    private String purpose;

    public VerifyOtpResponse() {}

    public VerifyOtpResponse(String tempToken, String purpose) {
        this.tempToken = tempToken;
        this.purpose = purpose;
    }

    public String getTempToken() { return tempToken; }
    public void setTempToken(String tempToken) { this.tempToken = tempToken; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
}
