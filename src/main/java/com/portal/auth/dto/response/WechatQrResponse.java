package com.portal.auth.dto.response;

public class WechatQrResponse {
    private String url;
    private String ticket;

    public WechatQrResponse() {}

    public WechatQrResponse(String url, String ticket) {
        this.url = url;
        this.ticket = ticket;
    }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getTicket() { return ticket; }
    public void setTicket(String ticket) { this.ticket = ticket; }
}
