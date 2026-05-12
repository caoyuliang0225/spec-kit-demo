package com.portal.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public class DeviceHeartbeatRequest {
    @NotBlank
    private String deviceId;

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }
}
