package com.portal.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public class WechatLoginRequest {
    @NotBlank
    private String code;

    @NotBlank
    private String channel;

    private String deviceId;
    private String deviceType;
    private String deviceName;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getDeviceType() { return deviceType; }
    public void setDeviceType(String deviceType) { this.deviceType = deviceType; }

    public String getDeviceName() { return deviceName; }
    public void setDeviceName(String deviceName) { this.deviceName = deviceName; }
}
