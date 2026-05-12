package com.portal.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class CaptchaService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final String secretKey;
    private final String provider;

    public CaptchaService(
            @Value("${app.captcha.secret-key}") String secretKey,
            @Value("${app.captcha.provider}") String provider) {
        this.secretKey = secretKey;
        this.provider = provider;
    }

    public boolean verify(String token) {
        if ("turnstile".equals(provider)) {
            return verifyTurnstile(token);
        }
        if ("recaptcha".equals(provider)) {
            return verifyRecaptcha(token);
        }
        if ("hcaptcha".equals(provider)) {
            return verifyHCaptcha(token);
        }
        return false;
    }

    private boolean verifyTurnstile(String token) {
        try {
            var url = "https://challenges.cloudflare.com/turnstile/v0/siteverify";
            var body = Map.of("secret", secretKey, "response", token);
            var response = restTemplate.postForObject(url, body, Map.class);
            return response != null && Boolean.TRUE.equals(response.get("success"));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifyRecaptcha(String token) {
        try {
            var url = "https://www.google.com/recaptcha/api/siteverify";
            var body = Map.of("secret", secretKey, "response", token);
            var response = restTemplate.postForObject(url, body, Map.class);
            return response != null && Boolean.TRUE.equals(response.get("success"));
        } catch (Exception e) {
            return false;
        }
    }

    private boolean verifyHCaptcha(String token) {
        try {
            var url = "https://hcaptcha.com/siteverify";
            var body = Map.of("secret", secretKey, "response", token);
            var response = restTemplate.postForObject(url, body, Map.class);
            return response != null && Boolean.TRUE.equals(response.get("success"));
        } catch (Exception e) {
            return false;
        }
    }
}
