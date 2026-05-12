package com.portal.auth.service;

import com.portal.auth.exception.AuthException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;

@Service
public class OtpService {

    private final StringRedisTemplate redis;
    private final JavaMailSender mailSender;
    private final CaptchaService captchaService;

    private final int otpLength;
    private final long ttlSeconds;
    private final long resendCooldownSeconds;
    private final int maxVerifyAttempts;
    private final int maxSendPerHour;

    private static final String OTP_KEY = "otp:code:";
    private static final String OTP_ATTEMPTS_KEY = "otp:attempts:";
    private static final String OTP_SEND_KEY = "otp:send:";
    private static final String OTP_COOLDOWN_KEY = "otp:cooldown:";
    private static final String OTP_CAPTCHA_KEY = "otp:captcha:";

    private final SecureRandom random = new SecureRandom();

    public OtpService(StringRedisTemplate redis,
                      JavaMailSender mailSender,
                      CaptchaService captchaService,
                      @Value("${app.otp.length}") int otpLength,
                      @Value("${app.otp.ttl-seconds}") long ttlSeconds,
                      @Value("${app.otp.resend-cooldown-seconds}") long resendCooldownSeconds,
                      @Value("${app.otp.max-verify-attempts}") int maxVerifyAttempts,
                      @Value("${app.otp.max-send-per-hour}") int maxSendPerHour) {
        this.redis = redis;
        this.mailSender = mailSender;
        this.captchaService = captchaService;
        this.otpLength = otpLength;
        this.ttlSeconds = ttlSeconds;
        this.resendCooldownSeconds = resendCooldownSeconds;
        this.maxVerifyAttempts = maxVerifyAttempts;
        this.maxSendPerHour = maxSendPerHour;
    }

    public void sendOtp(String account, String captchaToken) {
        if (!isEmail(account) && !isPhone(account)) {
            throw new AuthException("AUTH_400", "Invalid account format", "account");
        }

        boolean captchaAlreadyVerified = Boolean.TRUE.equals(
                redis.hasKey(OTP_CAPTCHA_KEY + account));

        if (!captchaAlreadyVerified) {
            if (!captchaService.verify(captchaToken)) {
                throw new AuthException("AUTH_002", "CAPTCHA verification failed", "captchaToken");
            }
            redis.opsForValue().set(OTP_CAPTCHA_KEY + account, "1", ttlSeconds, TimeUnit.SECONDS);
        }

        String sendCountStr = redis.opsForValue().get(OTP_SEND_KEY + account);
        int sendCount = sendCountStr != null ? Integer.parseInt(sendCountStr) : 0;
        if (sendCount >= maxSendPerHour) {
            throw new AuthException("AUTH_007", "OTP send limit reached. Try again later.", "account");
        }

        boolean onCooldown = Boolean.TRUE.equals(redis.hasKey(OTP_COOLDOWN_KEY + account));
        if (onCooldown) {
            throw new AuthException("AUTH_007", "Please wait before requesting a new OTP", "account");
        }

        String code = generateOtp();
        redis.opsForValue().set(OTP_KEY + account, code, ttlSeconds, TimeUnit.SECONDS);
        redis.opsForValue().set(OTP_COOLDOWN_KEY + account, "1", resendCooldownSeconds, TimeUnit.SECONDS);

        long hourTtl = redis.getExpire(OTP_SEND_KEY + account, TimeUnit.SECONDS);
        if (hourTtl <= 0) {
            redis.opsForValue().set(OTP_SEND_KEY + account, "1", 1, TimeUnit.HOURS);
        } else {
            redis.opsForValue().increment(OTP_SEND_KEY + account);
        }

        deliverOtp(account, code);
    }

    public void verifyOtp(String account, String otpCode, String purpose) {
        String storedCode = redis.opsForValue().getAndDelete(OTP_KEY + account);
        if (storedCode == null) {
            throw new AuthException("AUTH_001", "OTP not found or expired", "otpCode");
        }

        if (!storedCode.equals(otpCode)) {
            String attemptKey = OTP_ATTEMPTS_KEY + account;
            Long attempts = redis.opsForValue().increment(attemptKey);
            if (attempts != null && attempts == 1) {
                redis.expire(attemptKey, ttlSeconds, TimeUnit.SECONDS);
            }
            if (attempts != null && attempts >= maxVerifyAttempts) {
                redis.delete(OTP_KEY + account);
                redis.delete(OTP_ATTEMPTS_KEY + account);
                throw new AuthException("AUTH_001", "Too many failed attempts. Request a new OTP.", "otpCode");
            }
            throw new AuthException("AUTH_001", "Invalid OTP code", "otpCode");
        }

        redis.delete(OTP_ATTEMPTS_KEY + account);
        redis.delete(OTP_CAPTCHA_KEY + account);
        redis.delete(OTP_COOLDOWN_KEY + account);
    }

    private String generateOtp() {
        var sb = new StringBuilder(otpLength);
        for (int i = 0; i < otpLength; i++) {
            sb.append(random.nextInt(10));
        }
        return sb.toString();
    }

    private void deliverOtp(String account, String code) {
        if (isEmail(account)) {
            var msg = new SimpleMailMessage();
            msg.setTo(account);
            msg.setSubject("Your verification code");
            msg.setText("Your verification code is: " + code + "\nValid for " + ttlSeconds / 60 + " minutes.");
            mailSender.send(msg);
        }
    }

    private boolean isEmail(String account) {
        return account != null && account.contains("@");
    }

    private boolean isPhone(String account) {
        return account != null && account.matches("\\+?\\d{7,15}");
    }
}
