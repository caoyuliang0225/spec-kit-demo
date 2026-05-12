package com.portal.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {

    private final StringRedisTemplate redis;
    private final int qpsPerEndpoint;
    private final int loginMaxAttempts;
    private final long loginWindowSeconds;

    private static final String QPS_KEY = "ratelimit:qps:";
    private static final String LOGIN_KEY = "ratelimit:login:";

    public RateLimitService(StringRedisTemplate redis,
                            @Value("${app.rate-limit.qps-per-endpoint}") int qpsPerEndpoint,
                            @Value("${app.rate-limit.login-max-attempts}") int loginMaxAttempts,
                            @Value("${app.rate-limit.login-window-seconds}") long loginWindowSeconds) {
        this.redis = redis;
        this.qpsPerEndpoint = qpsPerEndpoint;
        this.loginMaxAttempts = loginMaxAttempts;
        this.loginWindowSeconds = loginWindowSeconds;
    }

    public boolean tryConsumeQps(String endpoint) {
        String key = QPS_KEY + endpoint;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, 1, TimeUnit.SECONDS);
        }
        return count != null && count <= qpsPerEndpoint;
    }

    public boolean checkLoginLimit(String account, String ip) {
        String key = LOGIN_KEY + account + ":" + ip;
        Long count = redis.opsForValue().increment(key);
        if (count != null && count == 1) {
            redis.expire(key, loginWindowSeconds, TimeUnit.SECONDS);
        }
        return count != null && count <= loginMaxAttempts;
    }

    public void resetLoginLimit(String account, String ip) {
        redis.delete(LOGIN_KEY + account + ":" + ip);
    }

    public int getQpsPerEndpoint() {
        return qpsPerEndpoint;
    }
}
