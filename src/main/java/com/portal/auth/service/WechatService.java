package com.portal.auth.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.portal.auth.exception.AuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class WechatService {

    private static final Logger log = LoggerFactory.getLogger(WechatService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final StringRedisTemplate redis;

    private final String openAppId;
    private final String openAppSecret;
    private final String openRedirectUri;
    private final String mpAppId;
    private final String mpAppSecret;
    private final String appAppId;
    private final String appAppSecret;

    private static final String QR_TICKET_KEY = "wechat:qr:";

    public WechatService(
            StringRedisTemplate redis,
            @Value("${app.wechat.open-platform.app-id:}") String openAppId,
            @Value("${app.wechat.open-platform.app-secret:}") String openAppSecret,
            @Value("${app.wechat.open-platform.redirect-uri:}") String openRedirectUri,
            @Value("${app.wechat.official-account.app-id:}") String mpAppId,
            @Value("${app.wechat.official-account.app-secret:}") String mpAppSecret,
            @Value("${app.wechat.app-sdk.app-id:}") String appAppId,
            @Value("${app.wechat.app-sdk.app-secret:}") String appAppSecret) {
        this.redis = redis;
        this.openAppId = openAppId;
        this.openAppSecret = openAppSecret;
        this.openRedirectUri = openRedirectUri;
        this.mpAppId = mpAppId;
        this.mpAppSecret = mpAppSecret;
        this.appAppId = appAppId;
        this.appAppSecret = appAppSecret;
    }

    public WechatTokenResult exchangeCode(String code, String channel) {
        String appId = getAppId(channel);
        String appSecret = getAppSecret(channel);
        String url = "https://api.weixin.qq.com/sns/oauth2/access_token"
                + "?appid=" + appId
                + "&secret=" + appSecret
                + "&code=" + code
                + "&grant_type=authorization_code";

        try {
            var response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                throw new AuthException("AUTH_020", "WeChat API returned empty response");
            }
            if (response.containsKey("errcode") && (int) response.get("errcode") != 0) {
                String errMsg = (String) response.getOrDefault("errmsg", "unknown");
                log.warn("WeChat code exchange failed: {} {}", response.get("errcode"), errMsg);
                throw new AuthException("AUTH_020", "WeChat auth failed: " + errMsg);
            }
            var result = new WechatTokenResult();
            result.accessToken = (String) response.get("access_token");
            result.openid = (String) response.get("openid");
            result.unionid = (String) response.get("unionid");
            result.expiresIn = (Integer) response.getOrDefault("expires_in", 7200);
            return result;
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("WeChat API call failed", e);
            throw new AuthException("AUTH_020", "WeChat auth code exchange failed");
        }
    }

    public WechatUserInfo fetchUserInfo(String accessToken, String openid) {
        String url = "https://api.weixin.qq.com/sns/userinfo"
                + "?access_token=" + accessToken
                + "&openid=" + openid
                + "&lang=zh_CN";

        try {
            var response = restTemplate.getForObject(url, Map.class);
            if (response == null) {
                throw new AuthException("AUTH_023", "WeChat userinfo API returned empty");
            }
            if (response.containsKey("errcode") && (int) response.get("errcode") != 0) {
                throw new AuthException("AUTH_023", "Failed to fetch WeChat userinfo");
            }
            var info = new WechatUserInfo();
            info.nickname = (String) response.getOrDefault("nickname", "");
            info.headimgurl = (String) response.getOrDefault("headimgurl", "");
            info.openid = (String) response.get("openid");
            info.unionid = (String) response.get("unionid");
            return info;
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch WeChat userinfo", e);
            throw new AuthException("AUTH_023", "WeChat userinfo fetch failed");
        }
    }

    public QrCodeResult getQrCodeUrl(String state) {
        if (openAppId.isEmpty() || openRedirectUri.isEmpty()) {
            throw new AuthException("AUTH_400", "WeChat Open Platform not configured");
        }
        String url = "https://open.weixin.qq.com/connect/qrconnect"
                + "?appid=" + openAppId
                + "&redirect_uri=" + openRedirectUri
                + "&response_type=code"
                + "&scope=snsapi_login"
                + "&state=" + state;

        var result = new QrCodeResult();
        result.url = url;
        result.ticket = state;
        return result;
    }

    public String createQrTicket() {
        String ticket = UUID.randomUUID().toString();
        redis.opsForValue().set(QR_TICKET_KEY + ticket, "pending", 5, TimeUnit.MINUTES);
        return ticket;
    }

    public void completeQrTicket(String ticket, String accessToken, String refreshToken) {
        String value = "done:" + accessToken + ":" + refreshToken;
        redis.opsForValue().set(QR_TICKET_KEY + ticket, value, 5, TimeUnit.MINUTES);
    }

    public String getQrStatus(String ticket) {
        String value = redis.opsForValue().get(QR_TICKET_KEY + ticket);
        if (value == null) {
            return "expired";
        }
        if ("pending".equals(value)) {
            return "pending";
        }
        return "completed";
    }

    public String getQrResultAccessToken(String ticket) {
        String value = redis.opsForValue().get(QR_TICKET_KEY + ticket);
        if (value != null && value.startsWith("done:")) {
            return value.split(":")[1];
        }
        return null;
    }

    public String getQrResultRefreshToken(String ticket) {
        String value = redis.opsForValue().get(QR_TICKET_KEY + ticket);
        if (value != null && value.startsWith("done:")) {
            String[] parts = value.split(":");
            return parts.length > 2 ? parts[2] : null;
        }
        return null;
    }

    public void deleteQrTicket(String ticket) {
        redis.delete(QR_TICKET_KEY + ticket);
    }

    private String getAppId(String channel) {
        return switch (channel) {
            case "official_account" -> mpAppId;
            case "app" -> appAppId;
            case "qr" -> openAppId;
            default -> throw new AuthException("AUTH_400", "Invalid WeChat channel: " + channel);
        };
    }

    private String getAppSecret(String channel) {
        return switch (channel) {
            case "official_account" -> mpAppSecret;
            case "app" -> appAppSecret;
            case "qr" -> openAppSecret;
            default -> throw new AuthException("AUTH_400", "Invalid WeChat channel: " + channel);
        };
    }

    public static class WechatTokenResult {
        private String accessToken;
        private String openid;
        private String unionid;
        private int expiresIn;

        public String getAccessToken() { return accessToken; }
        public String getOpenid() { return openid; }
        public String getUnionid() { return unionid; }
        public int getExpiresIn() { return expiresIn; }
    }

    public static class WechatUserInfo {
        private String nickname;
        private String headimgurl;
        private String openid;
        private String unionid;

        public String getNickname() { return nickname; }
        public String getHeadimgurl() { return headimgurl; }
        public String getOpenid() { return openid; }
        public String getUnionid() { return unionid; }
    }

    public static class QrCodeResult {
        private String url;
        private String ticket;

        public String getUrl() { return url; }
        public String getTicket() { return ticket; }
    }
}
