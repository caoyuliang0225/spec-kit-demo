package com.portal.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public class LoginRequest {
    @NotBlank
    private String account;

    @NotBlank
    private String credential;

    @NotBlank
    private String type;

    private String deviceId;
    private String deviceType;
    private String deviceName;

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }

    public String getCredential() { return credential; }
    public void setCredential(String credential) { this.credential = credential; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
}
