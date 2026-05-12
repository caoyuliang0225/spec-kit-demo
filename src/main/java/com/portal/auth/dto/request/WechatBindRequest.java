package com.portal.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public class WechatBindRequest {
    @NotBlank
    private String code;

    @NotBlank
    private String channel;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
}
